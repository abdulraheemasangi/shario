package org.zeroshare.app.data.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.core.hash.Sha256Hasher
import org.zeroshare.app.core.security.AesGcmCipher
import org.zeroshare.app.data.storage.RawContentReader
import org.zeroshare.app.domain.model.FileTransferItemState
import org.zeroshare.app.domain.model.ShareableItem
import org.zeroshare.app.domain.model.TransferManifest
import org.zeroshare.app.domain.model.TransferSessionState
import org.zeroshare.app.domain.model.TransferStatus
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.crypto.SecretKey

class SocketSenderEngine(
    private val rawContentReader: RawContentReader
) {
    @Volatile private var isPaused = false
    @Volatile private var isCancelled = false

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }
    fun cancel() { isCancelled = true }

    suspend fun sendFiles(
        host: String,
        port: Int,
        items: List<ShareableItem>,
        manifest: TransferManifest,
        secretKey: SecretKey?,
        onProgressUpdate: (TransferSessionState) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val socket = Socket()
        val speedCalculator = SpeedCalculator()

        try {
            socket.tcpNoDelay = true
            socket.sendBufferSize = 512 * 1024
            socket.connect(InetSocketAddress(host, port), 10000)

            val outStream = DataOutputStream(socket.getOutputStream())
            val inStream = DataInputStream(socket.getInputStream())
            val cipher = secretKey?.let { AesGcmCipher(it) }

            // 1. Handshake
            val handshakeJson = JSONObject().apply {
                put("protocol", ProtocolConstants.PROTOCOL_VERSION)
                put("device", manifest.senderDeviceName)
            }.toString().toByteArray(Charsets.UTF_8)
            ProtocolPacket(ProtocolConstants.TYPE_HANDSHAKE, payload = handshakeJson).writeTo(outStream)

            // 2. Manifest
            val manifestBytes = manifest.toJson().toString().toByteArray(Charsets.UTF_8)
            ProtocolPacket(ProtocolConstants.TYPE_MANIFEST, payload = manifestBytes).writeTo(outStream)

            var totalBytesSent: Long = 0
            val fileStates = manifest.files.map {
                FileTransferItemState(
                    fileId = it.fileId,
                    name = it.name,
                    size = it.size,
                    sha256Expected = it.sha256,
                    totalChunks = it.totalChunks
                )
            }.toMutableList()

            var sessionState = TransferSessionState(
                transferId = manifest.transferId,
                status = TransferStatus.TRANSFERRING,
                isSender = true,
                remoteDeviceName = host,
                totalFiles = manifest.totalFiles,
                totalBytes = manifest.totalBytes,
                fileStates = fileStates
            )
            onProgressUpdate(sessionState)

            // 3. Send Files
            for (fileIndex in manifest.files.indices) {
                if (isCancelled) break

                val metadata = manifest.files[fileIndex]
                val shareableItem = items[fileIndex]

                sessionState = sessionState.copy(
                    currentFileIndex = fileIndex + 1,
                    currentFileName = metadata.name
                )
                onProgressUpdate(sessionState)

                // Query resume offset from receiver
                ProtocolPacket(
                    ProtocolConstants.TYPE_RESUME_REQUEST,
                    fileId = metadata.fileId
                ).writeTo(outStream)

                val resumePacket = ProtocolPacket.readFrom(inStream)
                val resumeOffset = ResumeOffset.fromJson(JSONObject(String(resumePacket.payload, Charsets.UTF_8)))

                var fileBytesSent = resumeOffset.startByteOffset
                totalBytesSent += resumeOffset.startByteOffset

                val inputStream = rawContentReader.openInputStreamAtOffset(
                    shareableItem.uri,
                    resumeOffset.startByteOffset
                )

                val chunkBuffer = ByteArray(metadata.chunkSize)
                var currentChunkIndex = resumeOffset.nextChunkIndex

                inputStream.use { stream ->
                    var bytesRead: Int
                    while (stream.read(chunkBuffer).also { bytesRead = it } != -1) {
                        while (isPaused && !isCancelled) {
                            sessionState = sessionState.copy(status = TransferStatus.PAUSED)
                            onProgressUpdate(sessionState)
                            Thread.sleep(200)
                        }
                        if (isCancelled) break

                        val rawChunk = if (bytesRead == chunkBuffer.size) chunkBuffer else chunkBuffer.copyOf(bytesRead)
                        val payload = cipher?.encrypt(rawChunk) ?: rawChunk

                        val packet = ProtocolPacket(
                            type = ProtocolConstants.TYPE_CHUNK_DATA,
                            fileId = metadata.fileId,
                            chunkIndex = currentChunkIndex,
                            payload = payload
                        )
                        packet.writeTo(outStream)

                        fileBytesSent += bytesRead
                        totalBytesSent += bytesRead
                        currentChunkIndex++

                        val speed = speedCalculator.update(totalBytesSent)
                        val eta = speedCalculator.getEstimatedEta(manifest.totalBytes - totalBytesSent)

                        fileStates[fileIndex] = fileStates[fileIndex].copy(
                            bytesTransferred = fileBytesSent,
                            currentChunkIndex = currentChunkIndex
                        )

                        sessionState = sessionState.copy(
                            status = TransferStatus.TRANSFERRING,
                            bytesTransferred = totalBytesSent,
                            speedBytesPerSec = speed,
                            etaFormatted = eta,
                            fileStates = fileStates.toList()
                        )
                        onProgressUpdate(sessionState)
                    }
                }

                if (isCancelled) break

                // 4. File Complete Packet
                ProtocolPacket(ProtocolConstants.TYPE_FILE_COMPLETE, fileId = metadata.fileId).writeTo(outStream)

                // 5. Await SHA-256 Verification Ack
                sessionState = sessionState.copy(status = TransferStatus.VERIFYING)
                onProgressUpdate(sessionState)

                val ackPacket = ProtocolPacket.readFrom(inStream)
                val ackJson = JSONObject(String(ackPacket.payload, Charsets.UTF_8))
                val status = ackJson.getString("status")
                val actualHash = ackJson.optString("actualHash", "")

                val isVerified = status == ProtocolConstants.STATUS_VERIFIED
                fileStates[fileIndex] = fileStates[fileIndex].copy(
                    isVerified = isVerified,
                    sha256Actual = actualHash,
                    isFailed = !isVerified
                )

                sessionState = sessionState.copy(
                    fileStates = fileStates.toList()
                )
                onProgressUpdate(sessionState)
            }

            if (isCancelled) {
                ProtocolPacket(ProtocolConstants.TYPE_CANCEL).writeTo(outStream)
                onProgressUpdate(sessionState.copy(status = TransferStatus.CANCELLED))
            } else {
                onProgressUpdate(sessionState.copy(status = TransferStatus.COMPLETED))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            onProgressUpdate(
                TransferSessionState(
                    status = TransferStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Transfer failed"
                )
            )
            Result.failure(e)
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }
}
