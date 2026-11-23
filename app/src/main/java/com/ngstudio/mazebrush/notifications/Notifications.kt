package com.ngstudio.mazebrush.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ngstudio.mazebrush.MainActivity
import com.ngstudio.mazebrush.R

/** Channels, permission checks and the one place that builds every notification the app posts. */
object Notifications {
    const val CHANNEL_REMINDERS = "daily_reminders"
    /** Must match @string/channel_news_id, which the manifest hands to FCM as its default channel. */
    const val CHANNEL_NEWS = "news_events"

    /** Intent extra set on notification taps, so the app can tell where the open came from. */
    const val EXTRA_SOURCE = "notif_source"

    /** True while MainActivity is on screen; reminders stay quiet while the player is already playing. */
    @Volatile
    var appInForeground = false

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_reminders_desc) },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_NEWS, context.getString(R.string.channel_news_name), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.channel_news_desc) },
        )
    }

    /** Android 13+ needs the runtime POST_NOTIFICATIONS grant; older versions only the app-level switch. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun canPost(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Post a notification that opens the game when tapped. Silently does nothing without permission. */
    fun show(context: Context, id: Int, channel: String, title: String, body: String, source: String) {
        if (!canPost(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SOURCE, source)
        }
        val tap = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.notify_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(if (channel == CHANNEL_REMINDERS) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(if (channel == CHANNEL_NEWS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post.
        }
    }
}
