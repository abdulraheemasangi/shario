package org.zeroshare.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.zeroshare.app.R
import org.zeroshare.app.SharioApplication
import org.zeroshare.app.core.extensions.FormatExtensions
import org.zeroshare.app.domain.model.TransferSessionState
import org.zeroshare.app.presentation.MainActivity

class TransferNotificationManager(private val context: Context) {

    fun buildNotification(state: TransferSessionState): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, TransferService::class.java).apply {
                action = TransferService.ACTION_CANCEL
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressPercent = if (state.totalBytes > 0) {
            ((state.bytesTransferred.toDouble() / state.totalBytes) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val speed = FormatExtensions.formatSpeed(state.speedBytesPerSec)
        val contentText = "${FormatExtensions.formatBytes(state.bytesTransferred)} / ${FormatExtensions.formatBytes(state.totalBytes)} • $speed • ETA: ${state.etaFormatted}"

        val title = if (state.currentFileName.isNotEmpty()) {
            context.getString(R.string.notif_transferring_title, state.currentFileName)
        } else {
            context.getString(R.string.app_name)
        }

        return NotificationCompat.Builder(context, SharioApplication.CHANNEL_ID_TRANSFER)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.action_cancel), cancelIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
    }
}
