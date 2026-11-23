package com.ngstudio.mazebrush.notifications

import com.ngstudio.mazebrush.notifications.DailyReminders.Slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyRemindersTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(2026, 11, 23, h, m, s, 0, zone)
    private fun next(slot: Slot, now: ZonedDateTime) = Instant.ofEpochMilli(DailyReminders.nextTrigger(slot, now)).atZone(zone)

    @Test fun morningBeforeNineFiresToday() = assertEquals(at(9, 0), next(Slot.MORNING, at(8, 0)))

    @Test fun morningAfterNineFiresTomorrow() = assertEquals(at(9, 0).plusDays(1), next(Slot.MORNING, at(10, 48)))

    @Test fun eveningInTheMorningFiresTonight() = assertEquals(at(19, 0), next(Slot.EVENING, at(10, 48)))

    @Test fun rearmingJustAfterFiringSkipsToTomorrow() = assertEquals(at(19, 0).plusDays(1), next(Slot.EVENING, at(19, 0, 5)))

    @Test fun alarmDeliveredSlightlyEarlyStillSkipsToTomorrow() = assertEquals(at(19, 0).plusDays(1), next(Slot.EVENING, at(18, 59, 30)))

    @Test fun messagesChangeDailyAndMorningDiffersFromEvening() {
        val d = LocalDate.of(2026, 11, 23)
        assertNotEquals(DailyReminders.messageFor(Slot.MORNING, d), DailyReminders.messageFor(Slot.MORNING, d.plusDays(1)))
        assertNotEquals(DailyReminders.messageFor(Slot.MORNING, d).title, DailyReminders.messageFor(Slot.EVENING, d).title)
        // every message is distinct and short enough to read in the shade
        val all = DailyReminders.morning + DailyReminders.evening
        assertEquals(all.size, all.map { it.title }.toSet().size)
        assertTrue(all.all { it.title.length <= 45 && it.body.length <= 110 })
    }
}
