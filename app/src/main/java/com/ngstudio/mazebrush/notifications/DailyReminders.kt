package com.ngstudio.mazebrush.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Two local reminders a day, at 9:00 and 19:00 device time (ported from the studio's Ridge Riders).
 *
 * Timing: below Android 12 exact alarms need no permission. On Android 12+ exact alarms need the
 * user-granted SCHEDULE_EXACT_ALARM permission, which this app does not declare, so there it asks
 * for a 10-minute window instead (on time unless the phone is in deep Doze). Each alarm is one-shot
 * and re-arms itself for the next day when it fires; [ReminderBootReceiver] re-arms them after a
 * reboot, an app update or a clock change, and the app re-arms them on every launch, so they also
 * recover from a force-stop.
 */
object DailyReminders {

    enum class Slot(val hour: Int, val requestCode: Int, val notificationId: Int) {
        MORNING(hour = 9, requestCode = 9001, notificationId = 9001),
        EVENING(hour = 19, requestCode = 9019, notificationId = 9019),
    }

    const val ACTION_FIRE = "com.ngstudio.mazebrush.action.DAILY_REMINDER"
    const val EXTRA_SLOT = "slot"
    private const val WINDOW_MS = 10 * 60 * 1000L

    internal data class Message(val title: String, val body: String)

    // Morning: fresh start, the new Daily Challenge, a quick warm-up.
    internal val morning = listOf(
        Message("Good morning, painter! ☀️", "A brand-new Daily Challenge is waiting. Paint every tile before breakfast!"),
        Message("Rise and glide 🎨", "One swipe, one splash of colour. Wake up your brain with a quick maze."),
        Message("Today's puzzle is fresh 🧩", "The Daily Challenge just reset. Can you finish it at par?"),
        Message("Coffee + one maze = perfect morning ☕", "Clear a level before your coffee cools down."),
        Message("Your daily reward is ready 🎁", "Open the game to claim today's coins and keep your streak alive."),
        Message("Start the day with three stars ⭐", "Solve a level in the fewest moves and grab a perfect score."),
        Message("The ball is ready to roll 🟠", "Slide, bounce and splash. Your next level is one tap away."),
    )

    // Evening: unwind, beat your best, streaks and chests.
    internal val evening = listOf(
        Message("Unwind with a splash 🌙", "Relax tonight with a few calm, colourful mazes."),
        Message("Don't break your streak! 🔥", "You still have time to finish today's Daily Challenge."),
        Message("One more level before bed? 🛏️", "A tiny maze, a big splash of colour. You've got this!"),
        Message("A chest is getting closer 🧰", "Clear a few more levels tonight and open your next treasure chest."),
        Message("Beat your best tonight ⭐", "Go back to a level and chase three stars in fewer moves."),
        Message("Coins are waiting 🪙", "Earn coins tonight and unlock a shiny new ball skin."),
        Message("Golden hour, golden paint 🌅", "Wind down your evening by painting a maze or two."),
    )

    /** Arm both reminders (replacing any already set). */
    fun scheduleAll(context: Context) = Slot.entries.forEach { schedule(context, it) }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        Slot.entries.forEach { am.cancel(pendingIntent(context, it)) }
    }

    /** Arm [slot] for its next occurrence: later today if still ahead, otherwise tomorrow. */
    fun schedule(context: Context, slot: Slot) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val at = nextTrigger(slot)
        val pi = pendingIntent(context, slot)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            // A bounded window; a plain inexact alarm may be deferred by up to 75% of the time until it is due.
            am.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pi)
        }
    }

    fun post(context: Context, slot: Slot) {
        val msg = messageFor(slot, LocalDate.now())
        Notifications.show(
            context,
            id = slot.notificationId,
            channel = Notifications.CHANNEL_REMINDERS,
            title = msg.title,
            body = msg.body,
            source = "reminder_${slot.name.lowercase()}",
        )
    }

    internal fun nextTrigger(slot: Slot, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var at = now.with(LocalTime.of(slot.hour, 0))
        // A minute of slack so re-arming right after the alarm fires lands on tomorrow, not now.
        if (!at.isAfter(now.plusMinutes(1))) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    /** Rotates by day of year, so the text changes every day and morning and evening never repeat. */
    internal fun messageFor(slot: Slot, date: LocalDate): Message {
        val pool = if (slot == Slot.MORNING) morning else evening
        return pool[date.dayOfYear % pool.size]
    }

    private fun pendingIntent(context: Context, slot: Slot): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_SLOT, slot.name)
        return PendingIntent.getBroadcast(context, slot.requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
