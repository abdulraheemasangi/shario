package org.zeroshare.app.data.connection

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.zeroshare.app.core.security.ConnectionType
import org.zeroshare.app.domain.model.PeerDevice
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class UdpBroadcastDiscovery(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var broadcastJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    companion object {
        const val BROADCAST_PORT = 8989
        const val MAGIC_TAG = "SHARIO_P2P"
    }

    fun startAdvertising(deviceName: String, port: Int, scope: CoroutineScope) {
        stopAdvertising()
        broadcastJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                val broadcastAddress = InetAddress.getByName("255.255.255.255")

                val payload = JSONObject().apply {
                    put("tag", MAGIC_TAG)
                    put("name", deviceName)
                    put("port", port)
                    put("type", "LOCAL_WIFI")
                }.toString().toByteArray(Charsets.UTF_8)

                val packet = DatagramPacket(payload, payload.size, broadcastAddress, BROADCAST_PORT)

                while (isActive) {
                    try {
                        socket.send(packet)
                    } catch (ignored: Exception) {}
                    delay(1200)
                }
            } catch (ignored: Exception) {
            } finally {
                socket?.close()
            }
        }
    }

    fun stopAdvertising() {
        broadcastJob?.cancel()
        broadcastJob = null
    }

    fun discoverPeers(): Flow<List<PeerDevice>> = callbackFlow {
        acquireMulticastLock()

        val discovered = mutableMapOf<String, PeerDevice>()
        val scope = CoroutineScope(Dispatchers.IO)

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(BROADCAST_PORT).apply {
                broadcast = true
                soTimeout = 2000
                reuseAddress = true
            }
        } catch (e: Exception) {
            // If port is bound, try without port
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 2000
                }
            } catch (ignored: Exception) {}
        }

        val listenJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(2048)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive) {
                try {
                    socket?.receive(packet)
                    val rawStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    if (rawStr.contains(MAGIC_TAG)) {
                        val json = JSONObject(rawStr)
                        val name = json.optString("name", "Shario Receiver")
                        val port = json.optInt("port", 8988)
                        val senderIp = packet.address.hostAddress

                        if (!senderIp.isNullOrBlank() && !isLocalAddress(senderIp)) {
                            val peer = PeerDevice(
                                id = "udp_$senderIp",
                                name = name,
                                address = senderIp,
                                port = port,
                                connectionType = ConnectionType.SAME_NETWORK
                            )
                            discovered[peer.id] = peer
                            trySend(discovered.values.toList())
                        }
                    }
                } catch (ignored: SocketTimeoutException) {
                    // Periodic timeout to check isActive
                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
        }

        awaitClose {
            listenJob.cancel()
            socket?.close()
            releaseMulticastLock()
        }
    }

    private fun isLocalAddress(ip: String): Boolean {
        try {
            val addr = InetAddress.getByName(ip)
            return addr.isLoopbackAddress || addr.isAnyLocalAddress
        } catch (ignored: Exception) {
            return false
        }
    }

    private fun acquireMulticastLock() {
        try {
            multicastLock = wifiManager?.createMulticastLock("SharioMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (ignored: Exception) {}
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
            multicastLock = null
        } catch (ignored: Exception) {}
    }
}
