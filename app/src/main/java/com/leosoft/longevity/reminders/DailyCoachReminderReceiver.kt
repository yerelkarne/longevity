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
import com.leosoft.longevity.LongevityApp
import com.leosoft.longevity.MainActivity
import com.leosoft.longevity.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class DailyCoachEventType {
    LOGGING_REMINDER,
    EVENING_GOAL_CHECK
}

class DailyCoachReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eventTypeName = intent.getStringExtra(EXTRA_EVENT_TYPE).orEmpty()
                val eventType = runCatching { DailyCoachEventType.valueOf(eventTypeName) }
                    .getOrDefault(DailyCoachEventType.LOGGING_REMINDER)

                when (eventType) {
                    DailyCoachEventType.LOGGING_REMINDER -> {
                        sendNotification(
                            context = context,
                            notificationId = LOGGING_NOTIFICATION_ID,
                            title = context.getString(R.string.daily_logging_reminder_title),
                            message = context.getString(R.string.daily_logging_reminder_message)
                        )
                    }

                    DailyCoachEventType.EVENING_GOAL_CHECK -> {
                        val app = context.applicationContext as? LongevityApp
                        val repository = app?.repository
                        if (repository != null) {
                            val today = LocalDate.now()
                            val goals = repository.observeGoals().first() ?: return@launch
                            val dashboard = repository.observeDashboard(today).first()
                            val goalPlans = repository.observeGoalPlans().first()
                            val meals = repository.observeMealEntries(today).first()
                            val foodsById = repository.observeFoods().first().associateBy { it.id }

                            val deficits = DailyCoachReminderEvaluator.evaluateDeficits(
                                waterActualMl = dashboard.waterMl,
                                waterTargetMl = goals.waterTargetMl,
                                proteinActual = dashboard.macroTotals.protein,
                                proteinTarget = goals.proteinTarget,
                                carbsActual = dashboard.macroTotals.carbs,
                                carbsTarget = goals.carbsTarget,
                                fatActual = dashboard.macroTotals.fat,
                                fatTarget = goals.fatTarget,
                                fiberActual = dashboard.macroTotals.fiber,
                                fiberTarget = goals.fiberTarget,
                                meals = meals,
                                foodsById = foodsById,
                                micronutrientGoalPlans = goalPlans
                            )

                            val lines = buildList {
                                if (deficits.waterBehind) add(context.getString(R.string.daily_goal_water_behind))
                                if (deficits.macroBehind) add(context.getString(R.string.daily_goal_macro_behind))
                                if (deficits.microBehind) add(context.getString(R.string.daily_goal_micro_behind))
                            }

                            if (lines.isNotEmpty()) {
                                sendNotification(
                                    context = context,
                                    notificationId = GOAL_CHECK_NOTIFICATION_ID,
                                    title = context.getString(R.string.daily_goal_check_title),
                                    message = lines.joinToString("\n")
                                )
                            }
                        }
                    }
                }

                DailyCoachReminderScheduler.scheduleNext(context, eventType)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendNotification(context: Context, notificationId: Int, title: String, message: String) {
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
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(
            notificationId,
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .build()
        )
    }

    companion object {
        const val EXTRA_EVENT_TYPE = "extra_daily_coach_event_type"
        private const val LOGGING_NOTIFICATION_ID = 40_011
        private const val GOAL_CHECK_NOTIFICATION_ID = 40_020
    }
}

object DailyCoachReminderScheduler {
    private const val REQUEST_CODE_LOGGING = 5_011
    private const val REQUEST_CODE_GOAL_CHECK = 5_020

    fun scheduleAll(context: Context) {
        scheduleNext(context, DailyCoachEventType.LOGGING_REMINDER)
        scheduleNext(context, DailyCoachEventType.EVENING_GOAL_CHECK)
    }

    fun scheduleNext(context: Context, eventType: DailyCoachEventType) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = when (eventType) {
            DailyCoachEventType.LOGGING_REMINDER -> REQUEST_CODE_LOGGING
            DailyCoachEventType.EVENING_GOAL_CHECK -> REQUEST_CODE_GOAL_CHECK
        }

        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, DailyCoachReminderReceiver::class.java).apply {
                putExtra(DailyCoachReminderReceiver.EXTRA_EVENT_TYPE, eventType.name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = nextTriggerAt(LocalDateTime.now(), eventType)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    internal fun nextTriggerAt(now: LocalDateTime, eventType: DailyCoachEventType): LocalDateTime {
        return when (eventType) {
            DailyCoachEventType.LOGGING_REMINDER -> {
                val morning = now.withHour(11).withMinute(0).withSecond(0).withNano(0)
                val evening = now.withHour(20).withMinute(0).withSecond(0).withNano(0)
                when {
                    now.isBefore(morning) -> morning
                    now.isBefore(evening) -> evening
                    else -> now.plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0)
                }
            }
            DailyCoachEventType.EVENING_GOAL_CHECK -> {
                var evening = now.withHour(20).withMinute(0).withSecond(0).withNano(0)
                if (!evening.isAfter(now)) evening = evening.plusDays(1)
                evening
            }
        }
    }
}
