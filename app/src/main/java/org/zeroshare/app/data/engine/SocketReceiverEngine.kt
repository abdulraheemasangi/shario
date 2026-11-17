package org.zeroshare.app.data.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.core.hash.Sha256Hasher
import org.zeroshare.app.core.security.AesGcmCipher
import org.zeroshare.app.data.storage.ScopedStorageManager
import org.zeroshare.app.domain.model.FileTransferItemState
import org.zeroshare.app.domain.model.TransferManifest
import org.zeroshare.app.domain.model.TransferSessionState
import org.zeroshare.app.domain.model.TransferStatus
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.net.ServerSocket
import java.net.Socket
import javax.crypto.SecretKey

class SocketReceiverEngine(
    private val scopedStorageManager: ScopedStorageManager,
    private val chunkResumeManager: ChunkResumeManager = ChunkResumeManager()
) {
    @Volatile private var isCancelled = false
    private var serverSocket: ServerSocket? = null

    fun cancel() {
        isCancelled = true
        try { serverSocket?.close() } catch (ignored: Exception) {}
    }

    suspend fun startReceiving(
        port: Int = ProtocolConstants.DEFAULT_PORT,
        secretKey: SecretKey?,
        onProgressUpdate: (TransferSessionState) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        isCancelled = false
        val speedCalculator = SpeedCalculator()
        var clientSocket: Socket? = null

        try {
            try { serverSocket?.close() } catch (ignored: Exception) {}

            serverSocket = ServerSocket(port).apply {
                reuseAddress = true
            }

            // Wait for incoming sender connection. Keep status IDLE so QR screen stays visible!
            val socket = serverSocket!!.accept().also { clientSocket = it }
            socket.tcpNoDelay = true
            socket.receiveBufferSize = 512 * 1024

            val inStream = DataInputStream(socket.getInputStream())
            val outStream = DataOutputStream(socket.getOutputStream())
            val cipher = secretKey?.let { AesGcmCipher(it) }

            // 1. Handshake
            val handshakePacket = ProtocolPacket.readFrom(inStream)
            val handshakeJson = JSONObject(String(handshakePacket.payload, Charsets.UTF_8))
            val remoteDevice = handshakeJson.optString("device", "Sender Device")

            // 2. Manifest
            val manifestPacket = ProtocolPacket.readFrom(inStream)
            val manifest = TransferManifest.fromJson(JSONObject(String(manifestPacket.payload, Charsets.UTF_8)))

            var totalBytesReceived: Long = 0
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
                isSender = false,
                remoteDeviceName = remoteDevice,
                totalFiles = manifest.totalFiles,
                totalBytes = manifest.totalBytes,
                fileStates = fileStates
            )
            onProgressUpdate(sessionState)

            // 3. Receive each file
            for (fileIndex in manifest.files.indices) {
                if (isCancelled) break

                val metadata = manifest.files[fileIndex]
                sessionState = sessionState.copy(
                    currentFileIndex = fileIndex + 1,
                    currentFileName = metadata.name
                )
                onProgressUpdate(sessionState)

                // Handle Resume Negotiation
                val resumeReq = ProtocolPacket.readFrom(inStream)
                val partFile = scopedStorageManager.getStagingPartFile(metadata.fileId, metadata.name)
                val resumeOffset = chunkResumeManager.calculateResumeOffset(metadata.fileId, partFile, metadata.size)

                val resumeRespBytes = resumeOffset.toJson().toString().toByteArray(Charsets.UTF_8)
                ProtocolPacket(ProtocolConstants.TYPE_RESUME_RESPONSE, payload = resumeRespBytes).writeTo(outStream)

                var fileBytesReceived = resumeOffset.startByteOffset
                totalBytesReceived += resumeOffset.startByteOffset

                val sha256Hasher = Sha256Hasher()

                // If resuming existing part file, hash initial bytes to maintain continuous stream digest
                if (resumeOffset.startByteOffset > 0 && partFile.exists()) {
                    FileInputStream(partFile).use { fis ->
                        val buffer = ByteArray(64 * 1024)
                        var readRemaining = resumeOffset.startByteOffset
                        while (readRemaining > 0) {
                            val toRead = minOf(buffer.size.toLong(), readRemaining).toInt()
                            val r = fis.read(buffer, 0, toRead)
                            if (r == -1) break
                            sha256Hasher.update(buffer, 0, r)
                            readRemaining -= r
                        }
                    }
                }

                // Chunk loop
                while (fileBytesReceived < metadata.size && !isCancelled) {
                    val packet = ProtocolPacket.readFrom(inStream)
                    if (packet.type == ProtocolConstants.TYPE_CANCEL) {
                        isCancelled = true
                        break
                    }
                    if (packet.type == ProtocolConstants.TYPE_FILE_COMPLETE) {
                        break
                    }

                    val rawChunk = cipher?.decrypt(packet.payload) ?: packet.payload

                    scopedStorageManager.writeChunkAtOffset(
                        fileId = metadata.fileId,
                        fileName = metadata.name,
                        byteOffset = fileBytesReceived,
                        chunkData = rawChunk,
                        length = rawChunk.size
                    )

                    sha256Hasher.update(rawChunk, 0, rawChunk.size)
                    fileBytesReceived += rawChunk.size
                    totalBytesReceived += rawChunk.size

                    val speed = speedCalculator.update(totalBytesReceived)
                    val eta = speedCalculator.getEstimatedEta(manifest.totalBytes - totalBytesReceived)

                    fileStates[fileIndex] = fileStates[fileIndex].copy(
                        bytesTransferred = fileBytesReceived,
                        currentChunkIndex = packet.chunkIndex + 1
                    )

                    sessionState = sessionState.copy(
                        bytesTransferred = totalBytesReceived,
                        speedBytesPerSec = speed,
                        etaFormatted = eta,
                        fileStates = fileStates.toList()
                    )
                    onProgressUpdate(sessionState)
                }

                if (isCancelled) break

                // 4. SHA-256 Verification & Publishing
                sessionState = sessionState.copy(status = TransferStatus.VERIFYING)
                onProgressUpdate(sessionState)

                val computedHash = sha256Hasher.digestHex()
                val isVerified = Sha256Hasher.verifyHash(computedHash, metadata.sha256)

                if (isVerified) {
                    // Finalize Scoped Storage publication (zero-loss, preserve original bytes)
                    scopedStorageManager.finalizeAndPublishFile(metadata)
                }

                val ackJson = JSONObject().apply {
                    put("fileId", metadata.fileId)
                    put("status", if (isVerified) ProtocolConstants.STATUS_VERIFIED else ProtocolConstants.STATUS_MISMATCH)
                    put("actualHash", computedHash)
                }.toString().toByteArray(Charsets.UTF_8)

                ProtocolPacket(ProtocolConstants.TYPE_VERIFICATION_ACK, payload = ackJson).writeTo(outStream)

                fileStates[fileIndex] = fileStates[fileIndex].copy(
                    isVerified = isVerified,
                    sha256Actual = computedHash,
                    isFailed = !isVerified
                )
                sessionState = sessionState.copy(fileStates = fileStates.toList())
                onProgressUpdate(sessionState)
            }

            if (isCancelled) {
                onProgressUpdate(sessionState.copy(status = TransferStatus.CANCELLED))
            } else {
                onProgressUpdate(sessionState.copy(status = TransferStatus.COMPLETED))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (!isCancelled) {
                onProgressUpdate(
                    TransferSessionState(
                        status = TransferStatus.FAILED,
                        errorMessage = e.localizedMessage ?: "Receiver error"
                    )
                )
            }
            Result.failure(e)
        } finally {
            try { clientSocket?.close() } catch (ignored: Exception) {}
            try { serverSocket?.close() } catch (ignored: Exception) {}
        }
    }
}
