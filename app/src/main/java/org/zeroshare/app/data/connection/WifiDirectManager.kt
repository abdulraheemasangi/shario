package org.zeroshare.app.data.connection

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.zeroshare.app.core.security.ConnectionType
import org.zeroshare.app.domain.model.PeerDevice

class WifiDirectManager(private val context: Context) {

    private val p2pManager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val p2pChannel: WifiP2pManager.Channel? = p2pManager?.initialize(context, Looper.getMainLooper(), null)

    fun isWifiEnabled(): Boolean {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wm?.isWifiEnabled == true
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm?.isLocationEnabled == true
        } else {
            @Suppress("DEPRECATION")
            lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(): Flow<List<PeerDevice>> = callbackFlow {
        if (p2pManager == null || p2pChannel == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val peerListListener = WifiP2pManager.PeerListListener { peerList: WifiP2pDeviceList ->
            val peers = peerList.deviceList.map { device ->
                PeerDevice(
                    id = "p2p_${device.deviceAddress}",
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
                        p2pManager.requestPeers(p2pChannel, peerListListener)
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, intentFilter)
            }
        } catch (ignored: Exception) {}

        p2pManager.discoverPeers(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
                p2pManager.stopPeerDiscovery(p2pChannel, null)
            } catch (ignored: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun createGroup(onSuccess: (groupInfo: String) -> Unit, onFailure: (reason: Int) -> Unit) {
        if (p2pManager == null || p2pChannel == null) {
            onFailure(-1)
            return
        }
        p2pManager.createGroup(p2pChannel, object : WifiP2pManager.ActionListener {
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
        onConnected: (ipAddress: String) -> Unit,
        onFailure: (reason: Int) -> Unit
    ) {
        if (p2pManager == null || p2pChannel == null) {
            onFailure(-1)
            return
        }
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }

        p2pManager.connect(p2pChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                p2pManager.requestConnectionInfo(p2pChannel) { info ->
                    if (info != null && info.groupFormed) {
                        val host = info.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
                        onConnected(host)
                    }
                }
            }
            override fun onFailure(reason: Int) {
                onFailure(reason)
            }
        })
    }

    fun removeGroup() {
        if (p2pManager != null && p2pChannel != null) {
            p2pManager.removeGroup(p2pChannel, null)
        }
    }
}
