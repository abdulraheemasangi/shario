package org.zeroshare.app.data.connection

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class HotspotConfig(
    val ssid: String,
    val passphrase: String,
    val ipAddress: String = "192.168.43.1"
)

class LocalHotspotManager(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    fun startLocalHotspot(): Flow<Result<HotspotConfig>> = callbackFlow {
        val callback = object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                reservation = res
                val config = res.wifiConfiguration
                val hotspotInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val softApConfig = res.softApConfiguration
                    HotspotConfig(
                        ssid = softApConfig?.ssid ?: "Shario-Hotspot",
                        passphrase = softApConfig?.passphrase ?: ""
                    )
                } else {
                    @Suppress("DEPRECATION")
                    HotspotConfig(
                        ssid = config?.SSID ?: "Shario-Hotspot",
                        passphrase = config?.preSharedKey ?: ""
                    )
                }
                trySend(Result.success(hotspotInfo))
            }

            override fun onStopped() {
                // Hotspot stopped
            }

            override fun onFailed(reason: Int) {
                trySend(Result.failure(Exception("Failed to start LocalOnlyHotspot with error code: $reason")))
            }
        }

        try {
            wifiManager.startLocalOnlyHotspot(callback, null)
        } catch (e: Exception) {
            trySend(Result.failure(e))
        }

        awaitClose {
            stopLocalHotspot()
        }
    }

    fun stopLocalHotspot() {
        reservation?.close()
        reservation = null
    }
}
