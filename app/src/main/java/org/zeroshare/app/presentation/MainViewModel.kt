package org.zeroshare.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.core.hash.Sha256Hasher
import org.zeroshare.app.core.security.AesGcmCipher
import org.zeroshare.app.core.security.ConnectionType
import org.zeroshare.app.core.security.QrConnectionPayload
import org.zeroshare.app.data.appextractor.AppPackageExtractor
import org.zeroshare.app.data.connection.ConnectionCoordinator
import org.zeroshare.app.data.engine.SocketReceiverEngine
import org.zeroshare.app.data.engine.SocketSenderEngine
import org.zeroshare.app.data.storage.MediaPickerRepository
import org.zeroshare.app.data.storage.RawContentReader
import org.zeroshare.app.domain.model.FileMetadata
import org.zeroshare.app.domain.model.PeerDevice
import org.zeroshare.app.domain.model.ShareableItem
import org.zeroshare.app.domain.model.TransferManifest
import org.zeroshare.app.domain.model.TransferRecord
import org.zeroshare.app.domain.model.TransferSessionState
import org.zeroshare.app.domain.model.TransferStatus
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaPickerRepository: MediaPickerRepository,
    private val rawContentReader: RawContentReader,
    private val appPackageExtractor: AppPackageExtractor,
    private val connectionCoordinator: ConnectionCoordinator,
    private val socketSenderEngine: SocketSenderEngine,
    private val socketReceiverEngine: SocketReceiverEngine
) : ViewModel() {

    val photos = MutableStateFlow<List<ShareableItem>>(emptyList())
    val videos = MutableStateFlow<List<ShareableItem>>(emptyList())
    val audio = MutableStateFlow<List<ShareableItem>>(emptyList())
    val documents = MutableStateFlow<List<ShareableItem>>(emptyList())
    val apps = MutableStateFlow<List<ShareableItem>>(emptyList())

    val discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val receiverQrPayload = MutableStateFlow<QrConnectionPayload?>(null)

    private val _transferSessionState = MutableStateFlow(TransferSessionState())
    val transferSessionState: StateFlow<TransferSessionState> = _transferSessionState.asStateFlow()

    val historyRecords = MutableStateFlow<List<TransferRecord>>(emptyList())

    fun loadMedia() {
        viewModelScope.launch {
            photos.value = mediaPickerRepository.queryPhotos()
            videos.value = mediaPickerRepository.queryVideos()
            audio.value = mediaPickerRepository.queryAudio()
            documents.value = mediaPickerRepository.queryDocuments()
            apps.value = mediaPickerRepository.queryInstalledApps()
        }
    }

    fun startDiscovery() {
        viewModelScope.launch {
            connectionCoordinator.startDiscovery().collect { peers ->
                discoveredPeers.value = peers
            }
        }
    }

    fun prepareToReceive(deviceName: String) {
        _transferSessionState.value = TransferSessionState(status = TransferStatus.IDLE)
        val payload = connectionCoordinator.prepareReceiverSession(deviceName, ConnectionType.WIFI_DIRECT)
        receiverQrPayload.value = payload

        viewModelScope.launch {
            val key = AesGcmCipher.keyFromBase64(payload.aesKeyBase64)
            socketReceiverEngine.startReceiving(
                port = payload.port,
                secretKey = key,
                onProgressUpdate = { state ->
                    _transferSessionState.value = state
                    if (state.status == TransferStatus.COMPLETED) {
                        recordHistory(state)
                    }
                }
            )
        }
    }

    fun startTransferToPeer(peer: PeerDevice, selectedItems: List<ShareableItem>, secretKeyBase64: String?) {
        viewModelScope.launch {
            // Build manifest and calculate SHA-256 for each item
            val filesList = mutableListOf<FileMetadata>()
            var totalBytes = 0L

            selectedItems.forEachIndexed { index, item ->
                val stream = rawContentReader.openOriginalInputStream(item.uri)
                val sha256 = Sha256Hasher.computeSha256(stream)
                val size = if (item.size > 0) item.size else rawContentReader.getExactFileSize(item.uri)
                totalBytes += size

                filesList.add(
                    FileMetadata(
                        fileId = index + 1,
                        name = item.name,
                        size = size,
                        mimeType = item.mimeType,
                        sha256 = sha256,
                        chunkSize = ProtocolConstants.CHUNK_SIZE_BYTES,
                        isAppBundle = item.isAppBundle
                    )
                )
            }

            val manifest = TransferManifest(
                transferId = UUID.randomUUID().toString(),
                senderDeviceName = "Sender Device",
                totalFiles = filesList.size,
                totalBytes = totalBytes,
                files = filesList
            )

            val secretKey = secretKeyBase64?.let { AesGcmCipher.keyFromBase64(it) }

            socketSenderEngine.sendFiles(
                host = peer.address,
                port = peer.port,
                items = selectedItems,
                manifest = manifest,
                secretKey = secretKey,
                onProgressUpdate = { state ->
                    _transferSessionState.value = state
                    if (state.status == TransferStatus.COMPLETED) {
                        recordHistory(state)
                    }
                }
            )
        }
    }

    fun pauseTransfer() {
        socketSenderEngine.pause()
    }

    fun resumeTransfer() {
        socketSenderEngine.resume()
    }

    fun cancelTransfer() {
        socketSenderEngine.cancel()
        socketReceiverEngine.cancel()
        _transferSessionState.value = TransferSessionState(status = TransferStatus.IDLE)
        receiverQrPayload.value = null
    }

    private fun recordHistory(state: TransferSessionState) {
        val newRecords = state.fileStates.map {
            TransferRecord(
                id = UUID.randomUUID().toString(),
                fileName = it.name,
                fileSize = it.size,
                mimeType = "application/octet-stream",
                sha256 = it.sha256Actual ?: it.sha256Expected,
                isVerified = it.isVerified,
                isIncoming = !state.isSender,
                peerName = state.remoteDeviceName
            )
        }
        historyRecords.value = newRecords + historyRecords.value
    }
}
