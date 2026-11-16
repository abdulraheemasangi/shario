package org.zeroshare.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.zeroshare.app.domain.model.TransferSessionState
import org.zeroshare.app.domain.model.TransferStatus

class TransferService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var notificationManager: TransferNotificationManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val _transferState = MutableStateFlow(TransferSessionState())
    val transferState: StateFlow<TransferSessionState> = _transferState.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): TransferService = this@TransferService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = TransferNotificationManager(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Shario::TransferWakeLock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours max guard
                val notification = notificationManager.buildNotification(_transferState.value)
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        0
                    }
                )
            }
            ACTION_CANCEL -> {
                updateState(_transferState.value.copy(status = TransferStatus.CANCELLED))
                stopForegroundService()
            }
        }
        return START_NOT_STICKY
    }

    fun updateState(newState: TransferSessionState) {
        _transferState.value = newState
        val notification = notificationManager.buildNotification(newState)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)

        if (newState.status == TransferStatus.COMPLETED ||
            newState.status == TransferStatus.FAILED ||
            newState.status == TransferStatus.CANCELLED) {
            stopForegroundService()
        }
    }

    private fun stopForegroundService() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_FOREGROUND = "org.zeroshare.app.action.START_FOREGROUND"
        const val ACTION_CANCEL = "org.zeroshare.app.action.CANCEL"
    }
}
