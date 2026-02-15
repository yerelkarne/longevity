package com.leosoft.longevity.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.leosoft.longevity.workers.SleepReminderWorker
import com.leosoft.longevity.workers.SupplementReminderWorker
import com.leosoft.longevity.workers.WaterReminderWorker
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleWaterReminders() {
        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(2, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("water_reminder", ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleSupplementReminder() {
        val request = PeriodicWorkRequestBuilder<SupplementReminderWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("supplement_reminder", ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleSleepReminder() {
        val request = PeriodicWorkRequestBuilder<SleepReminderWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("sleep_reminder", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
