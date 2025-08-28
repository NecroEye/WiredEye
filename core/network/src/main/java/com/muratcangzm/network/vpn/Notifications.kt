package com.muratcangzm.network.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object Notifications {
    private const val CHANNEL_ID = "vpn_monitor"
    fun vpn(ctx: Context): Notification {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Monitoring", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Monitoring DNS metadata")
            .setContentText("Active — metadata only")
            .setOngoing(true)
            .build()
    }
}
