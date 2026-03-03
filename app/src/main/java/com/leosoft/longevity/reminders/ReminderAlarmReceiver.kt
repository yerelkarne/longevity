package com.leosoft.longevity.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.leosoft.longevity.MainActivity
import com.leosoft.longevity.R
import com.leosoft.longevity.ui.theme.AppImageAssets
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { context.getString(R.string.reminder_default_title) }
        val reminderId = intent.getLongExtra(EXTRA_ID, 0L)
        val cadence = intent.getStringExtra(EXTRA_CADENCE).orEmpty()
        val intervalHours = intent.getIntExtra(EXTRA_INTERVAL_HOURS, 1)
        val reminderTime = intent.getStringExtra(EXTRA_TIME).orEmpty()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, context.getString(R.string.tab_reminders), NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = ((reminderId * 1_000_003L) xor System.currentTimeMillis()).toInt()

        nm.notify(
            notificationId,
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(AppImageAssets.notificationSmallIconRes)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .build()
        )

        if (reminderId > 0L) {
            ReminderAlarmScheduler.schedule(
                context = context,
                id = reminderId,
                title = title,
                cadence = cadence,
                reminderTime = reminderTime,
                intervalHours = intervalHours
            )
        }
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CADENCE = "extra_cadence"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_INTERVAL_HOURS = "extra_interval_hours"
    }
}

object ReminderAlarmScheduler {
    internal fun nextTriggerAt(
        now: LocalDateTime,
        cadence: String,
        reminderTime: String,
        intervalHours: Int
    ): LocalDateTime {
        return when (cadence) {
            "hourly" -> {
                val hourInterval = intervalHours.coerceAtLeast(1)
                val currentHourBoundary = now.withMinute(0).withSecond(0).withNano(0)
                currentHourBoundary.plusHours(hourInterval.toLong())
            }
            else -> {
                val t = runCatching { LocalTime.parse(reminderTime) }.getOrElse { LocalTime.of(9, 0) }
                var next = now.withHour(t.hour).withMinute(t.minute).withSecond(0).withNano(0)
                if (!next.isAfter(now)) next = next.plusDays(1)
                next
            }
        }
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    fun schedule(
        context: Context,
        id: Long,
        title: String,
        cadence: String,
        reminderTime: String,
        intervalHours: Int
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_ID, id)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_CADENCE, cadence)
            putExtra(ReminderAlarmReceiver.EXTRA_TIME, reminderTime)
            putExtra(ReminderAlarmReceiver.EXTRA_INTERVAL_HOURS, intervalHours)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = LocalDateTime.now()
        val nextTriggerAt = nextTriggerAt(now, cadence, reminderTime, intervalHours)
        val triggerAtMillis = nextTriggerAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        am.cancel(pi)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms() -> {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
            else -> {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        }
    }
}
