package org.zeroshare.app.data.connection

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.zeroshare.app.core.security.ConnectionType
import org.zeroshare.app.domain.model.PeerDevice

class WifiDirectManager(private val context: Context) {

    private val p2pManager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? = p2pManager?.initialize(context, Looper.getMainLooper(), null)

    @SuppressLint("MissingPermission")
    fun discoverPeers(): Flow<List<PeerDevice>> = callbackFlow {
        if (p2pManager == null || channel == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val peerListListener = WifiP2pManager.PeerListListener { peerList: WifiP2pDeviceList ->
            val peers = peerList.deviceList.map { device ->
                PeerDevice(
                    id = device.deviceAddress,
                    name = device.deviceName.ifBlank { "Direct Peer" },
                    address = device.deviceAddress,
                    connectionType = ConnectionType.WIFI_DIRECT,
                    isGroupOwner = device.isGroupOwner
                )
            }
            trySend(peers)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        p2pManager.requestPeers(channel, peerListListener)
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }

        context.registerReceiver(receiver, intentFilter)

        p2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
                p2pManager.stopPeerDiscovery(channel, null)
            } catch (ignored: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun createGroup(onSuccess: (groupInfo: String) -> Unit, onFailure: (reason: Int) -> Unit) {
        if (p2pManager == null || channel == null) {
            onFailure(-1)
            return
        }
        p2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                onSuccess("Wi-Fi Direct Group Created")
            }
            override fun onFailure(reason: Int) {
                onFailure(reason)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(
        deviceAddress: String,
        onConnected: (info: WifiP2pInfo) -> Unit,
        onFailure: (reason: Int) -> Unit
    ) {
        if (p2pManager == null || channel == null) {
            onFailure(-1)
            return
        }
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }

        p2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Monitor connection info
                p2pManager.requestConnectionInfo(channel) { info ->
                    if (info.groupFormed) {
                        onConnected(info)
                    }
                }
            }
            override fun onFailure(reason: Int) {
                onFailure(reason)
            }
        })
    }

    fun removeGroup() {
        if (p2pManager != null && channel != null) {
            p2pManager.removeGroup(channel, null)
        }
    }
}
