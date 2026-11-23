package com.ngstudio.mazebrush.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ngstudio.mazebrush.MazeApp

/**
 * Fires at 9:00 / 19:00: posts the reminder and re-arms the same slot for tomorrow. There is no in-game
 * switch; players mute reminders through Android's notification settings (channel "Daily reminders").
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyReminders.ACTION_FIRE) return
        val slot = intent.getStringExtra(DailyReminders.EXTRA_SLOT)
            ?.let { runCatching { DailyReminders.Slot.valueOf(it) }.getOrNull() } ?: return
        val app = context.applicationContext
        DailyReminders.schedule(app, slot)
        // Don't interrupt someone who is already playing.
        if (Notifications.appInForeground || !Notifications.canPost(app)) return
        DailyReminders.post(app, slot)
        runCatching { MazeApp.instance.telemetry.event("reminder_shown", "slot" to slot.name.lowercase()) }
    }
}

/** Alarms are wiped by a reboot and go stale after an update or a clock change, so re-arm them. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }
        val app = context.applicationContext
        DailyReminders.scheduleAll(app)
    }
}
