package org.zeroshare.app.data.connection

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.core.security.AesGcmCipher
import org.zeroshare.app.core.security.ConnectionType
import org.zeroshare.app.core.security.QrConnectionPayload
import org.zeroshare.app.domain.model.PeerDevice
import java.net.NetworkInterface

class ConnectionCoordinator(
    private val context: Context,
    val wifiDirectManager: WifiDirectManager,
    val localHotspotManager: LocalHotspotManager,
    val nsdDiscoveryManager: NsdDiscoveryManager,
    val udpBroadcastDiscovery: UdpBroadcastDiscovery
) {
    private val coordinatorScope = CoroutineScope(Dispatchers.IO)

    fun isWifiEnabled(): Boolean = wifiDirectManager.isWifiEnabled()
    fun isLocationEnabled(): Boolean = wifiDirectManager.isLocationEnabled()

    fun startDiscovery(): Flow<List<PeerDevice>> {
        val p2pFlow = wifiDirectManager.discoverPeers()
        val nsdFlow = nsdDiscoveryManager.discoverServices()
        val udpFlow = udpBroadcastDiscovery.discoverPeers()

        return combine(p2pFlow, nsdFlow, udpFlow) { p2pList, nsdList, udpList ->
            val combined = mutableMapOf<String, PeerDevice>()
            // Priority: UDP & NSD (direct IP addresses) then P2P
            udpList.forEach { combined[it.address] = it }
            nsdList.forEach { combined[it.address] = it }
            p2pList.forEach {
                if (!combined.containsKey(it.address)) {
                    combined[it.address] = it
                }
            }
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

        // 1. Broadcast presence via UDP on local network / hotspot
        udpBroadcastDiscovery.startAdvertising(deviceName, ProtocolConstants.DEFAULT_PORT, coordinatorScope)

        // 2. Register mDNS / NSD service
        nsdDiscoveryManager.registerService(ProtocolConstants.DEFAULT_PORT, deviceName)

        // 3. Start Wi-Fi Direct presence if applicable
        try {
            wifiDirectManager.createGroup(
                onSuccess = {},
                onFailure = {}
            )
        } catch (ignored: Exception) {}

        return QrConnectionPayload(
            version = ProtocolConstants.PROTOCOL_VERSION,
            connectionType = connectionType,
            ipAddress = ipAddress,
            port = ProtocolConstants.DEFAULT_PORT,
            aesKeyBase64 = keyBase64,
            receiverDeviceName = deviceName
        )
    }

    fun stopReceiverSession() {
        udpBroadcastDiscovery.stopAdvertising()
        try {
            wifiDirectManager.removeGroup()
        } catch (ignored: Exception) {}
    }

    fun getLocalIpAddress(): String? {
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
