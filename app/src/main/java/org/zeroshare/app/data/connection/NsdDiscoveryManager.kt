package org.zeroshare.app.data.connection

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.core.security.ConnectionType
import org.zeroshare.app.domain.model.PeerDevice

class NsdDiscoveryManager(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun registerService(port: Int, deviceName: String) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Shario-$deviceName"
            serviceType = ProtocolConstants.DEFAULT_NSD_SERVICE_TYPE
            this.port = port
        }

        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {}
            }
        )
    }

    fun discoverServices(): Flow<List<PeerDevice>> = callbackFlow {
        val discoveredPeers = mutableMapOf<String, PeerDevice>()

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("shario")) {
                    resolveService(service) { resolvedDevice ->
                        discoveredPeers[resolvedDevice.id] = resolvedDevice
                        trySend(discoveredPeers.values.toList())
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                discoveredPeers.remove(service.serviceName)
                trySend(discoveredPeers.values.toList())
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(
            ProtocolConstants.DEFAULT_NSD_SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (ignored: Exception) {}
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo, onResolved: (PeerDevice) -> Unit) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                val host = resolvedInfo.host?.hostAddress ?: return
                val port = resolvedInfo.port
                val device = PeerDevice(
                    id = resolvedInfo.serviceName,
                    name = resolvedInfo.serviceName.removePrefix("Shario-"),
                    address = host,
                    port = port,
                    connectionType = ConnectionType.SAME_NETWORK
                )
                onResolved(device)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            nsdManager.resolveService(serviceInfo, context.mainExecutor, resolveListener)
        } else {
            @Suppress("DEPRECATION")
            nsdManager.resolveService(serviceInfo, resolveListener)
        }
    }
}
