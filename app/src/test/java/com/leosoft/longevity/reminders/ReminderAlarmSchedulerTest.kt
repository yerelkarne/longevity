package com.leosoft.longevity.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ReminderAlarmSchedulerTest {
    @Test
    fun `hourly cadence schedules every hour when interval is 1`() {
        val now = LocalDateTime.of(2026, 1, 10, 10, 15, 0)

        val next = ReminderAlarmScheduler.nextTriggerAt(
            now = now,
            cadence = "hourly",
            reminderTime = "09:00",
            intervalHours = 1
        )

        assertEquals(LocalDateTime.of(2026, 1, 10, 11, 0, 0), next)
    }

    @Test
    fun `hourly cadence schedules every two hours when interval is 2`() {
        val now = LocalDateTime.of(2026, 1, 10, 10, 15, 0)

        val next = ReminderAlarmScheduler.nextTriggerAt(
            now = now,
            cadence = "hourly",
            reminderTime = "09:00",
            intervalHours = 2
        )

        assertEquals(LocalDateTime.of(2026, 1, 10, 12, 0, 0), next)
    }

    @Test
    fun `daily cadence schedules next day when time has passed`() {
        val now = LocalDateTime.of(2026, 1, 10, 19, 0, 0)

        val next = ReminderAlarmScheduler.nextTriggerAt(
            now = now,
            cadence = "daily",
            reminderTime = "18:30",
            intervalHours = 1
        )

        assertEquals(LocalDateTime.of(2026, 1, 11, 18, 30, 0), next)
    }
}
