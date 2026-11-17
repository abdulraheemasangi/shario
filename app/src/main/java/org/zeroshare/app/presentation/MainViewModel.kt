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
    val connectionCoordinator: ConnectionCoordinator,
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

    val isWifiEnabled = MutableStateFlow(true)
    val isLocationEnabled = MutableStateFlow(true)

    private val _transferSessionState = MutableStateFlow(TransferSessionState())
    val transferSessionState: StateFlow<TransferSessionState> = _transferSessionState.asStateFlow()

    val historyRecords = MutableStateFlow<List<TransferRecord>>(emptyList())

    fun refreshConnectivityStatus() {
        isWifiEnabled.value = connectionCoordinator.isWifiEnabled()
        isLocationEnabled.value = connectionCoordinator.isLocationEnabled()
    }

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
        refreshConnectivityStatus()
        viewModelScope.launch {
            connectionCoordinator.startDiscovery().collect { peers ->
                discoveredPeers.value = peers
            }
        }
    }

    fun prepareToReceive(deviceName: String) {
        refreshConnectivityStatus()
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
        if (peer.connectionType == ConnectionType.WIFI_DIRECT) {
            _transferSessionState.value = TransferSessionState(
                status = TransferStatus.CONNECTING,
                currentFileName = "Pairing with ${peer.name}..."
            )
            connectionCoordinator.wifiDirectManager.connectToPeer(
                deviceAddress = peer.address,
                onConnected = { resolvedIp ->
                    proceedWithSocketSend(resolvedIp, peer.port, selectedItems, secretKeyBase64)
                },
                onFailure = {
                    _transferSessionState.value = TransferSessionState(
                        status = TransferStatus.FAILED,
                        errorMessage = "Could not form Wi-Fi Direct link with ${peer.name}. Please scan QR code instead."
                    )
                }
            )
        } else {
            proceedWithSocketSend(peer.address, peer.port, selectedItems, secretKeyBase64)
        }
    }

    private fun proceedWithSocketSend(
        hostIp: String,
        port: Int,
        selectedItems: List<ShareableItem>,
        secretKeyBase64: String?
    ) {
        viewModelScope.launch {
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
                host = hostIp,
                port = port,
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
        connectionCoordinator.stopReceiverSession()
        _transferSessionState.value = TransferSessionState(status = TransferStatus.CANCELLED)
    }

    private fun recordHistory(state: TransferSessionState) {
        val firstFile = state.fileStates.firstOrNull()
        val newRecord = TransferRecord(
            id = if (state.transferId.isNotBlank()) state.transferId else UUID.randomUUID().toString(),
            fileName = state.currentFileName.ifBlank { firstFile?.name ?: "Transferred Files (${state.totalFiles})" },
            fileSize = state.totalBytes,
            mimeType = "application/octet-stream",
            sha256 = firstFile?.sha256Expected ?: "",
            isVerified = firstFile?.isVerified ?: (state.status == TransferStatus.COMPLETED),
            isIncoming = !state.isSender,
            peerName = state.remoteDeviceName.ifBlank { "Completed Peer" },
            timestamp = System.currentTimeMillis()
        )
        historyRecords.value = listOf(newRecord) + historyRecords.value
    }
}
