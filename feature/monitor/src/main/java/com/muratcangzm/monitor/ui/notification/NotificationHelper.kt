package com.muratcangzm.monitor.ui.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.muratcangzm.resources.R as Res
import com.muratcangzm.monitor.common.SpeedMode
import com.muratcangzm.monitor.model.ACTION_STOP_ENGINE
import com.muratcangzm.monitor.model.NOTIFICATION_CHANNEL_ID
import com.muratcangzm.monitor.model.NOTIFICATION_ID
import com.muratcangzm.monitor.utils.humanBytes
import java.util.Locale

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun updateRunningNotification(
    ctx: Context,
    running: Boolean,
    kbs: Double,
    pps: Double,
    mode: SpeedMode,
    total: Long
) {
    val nm = NotificationManagerCompat.from(ctx)
    if (!running) {
        nm.cancel(NOTIFICATION_ID)
        return
    }
    val ch = NotificationChannel(NOTIFICATION_CHANNEL_ID, ctx.getString(Res.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW)
    ch.enableVibration(false)
    ch.enableLights(false)
    ch.setShowBadge(false)
    val sys = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    sys.createNotificationChannel(ch)
    val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    val pi = PendingIntent.getActivity(
        ctx,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val stopIntent = Intent(ACTION_STOP_ENGINE).setPackage(ctx.packageName)
    val stopPi = PendingIntent.getBroadcast(
        ctx,
        1,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val modeText = when (mode) {
        SpeedMode.ECO -> ctx.getString(Res.string.speed_eco)
        SpeedMode.FAST -> ctx.getString(Res.string.speed_fast)
        SpeedMode.TURBO -> ctx.getString(Res.string.speed_turbo)
    }
    val content = ctx.getString(
        Res.string.notif_content,
        humanBytes(total),
        kbs,
        pps,
        modeText
    )
    val notif = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle(ctx.getString(Res.string.notif_running_title))
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setColor(0xFF7BD7FF.toInt())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(pi)
        .addAction(android.R.drawable.ic_media_pause, ctx.getString(Res.string.notif_stop), stopPi)
        .build()
    nm.notify(NOTIFICATION_ID, notif)
}