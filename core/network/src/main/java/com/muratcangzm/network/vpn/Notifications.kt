package com.muratcangzm.network.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object Notifications {
    const val CHANNEL_ID = "vpn_monitor"
    const val NOTIF_ID = 2001

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    fun build(
        ctx: Context,
        title: String,
        text: String,
    ): Notification {
        ensureChannel(ctx)
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }
}
