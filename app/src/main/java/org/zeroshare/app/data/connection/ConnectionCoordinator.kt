package org.zeroshare.app.data.connection

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.core.security.AesGcmCipher
import org.zeroshare.app.core.security.ConnectionType
import org.zeroshare.app.core.security.QrConnectionPayload
import org.zeroshare.app.core.security.QrPayloadCodec
import org.zeroshare.app.domain.model.PeerDevice
import java.net.InetAddress
import java.net.NetworkInterface

class ConnectionCoordinator(
    private val context: Context,
    private val wifiDirectManager: WifiDirectManager,
    private val localHotspotManager: LocalHotspotManager,
    private val nsdDiscoveryManager: NsdDiscoveryManager
) {

    fun startDiscovery(): Flow<List<PeerDevice>> {
        val p2pFlow = wifiDirectManager.discoverPeers()
        val nsdFlow = nsdDiscoveryManager.discoverServices()

        return combine(p2pFlow, nsdFlow) { p2pList, nsdList ->
            val combined = mutableMapOf<String, PeerDevice>()
            p2pList.forEach { combined[it.address] = it }
            nsdList.forEach { combined[it.address] = it }
            combined.values.toList()
        }
    }

    fun prepareReceiverSession(
        deviceName: String,
        connectionType: ConnectionType = ConnectionType.WIFI_DIRECT
    ): QrConnectionPayload {
        val ephemeralKey = AesGcmCipher.generateKey()
        val keyBase64 = AesGcmCipher.keyToBase64(ephemeralKey)
        val ipAddress = getLocalIpAddress() ?: "192.168.49.1"

        if (connectionType == ConnectionType.SAME_NETWORK) {
            nsdDiscoveryManager.registerService(ProtocolConstants.DEFAULT_PORT, deviceName)
        }

        return QrConnectionPayload(
            version = ProtocolConstants.PROTOCOL_VERSION,
            connectionType = connectionType,
            ipAddress = ipAddress,
            port = ProtocolConstants.DEFAULT_PORT,
            aesKeyBase64 = keyBase64,
            receiverDeviceName = deviceName
        )
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }
}
