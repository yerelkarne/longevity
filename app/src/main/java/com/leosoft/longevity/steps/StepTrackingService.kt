package com.leosoft.longevity.steps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.leosoft.longevity.LongevityApp
import com.leosoft.longevity.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StepTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var trackerManager: StepTrackerManager

    override fun onCreate() {
        super.onCreate()
        val app = application as LongevityApp
        trackerManager = StepTrackerManager(this, app.repository, app.preferences)
        startForeground(NOTIFICATION_ID, buildNotification())
        trackerManager.start()
        serviceScope.launch { app.preferences.setForegroundTrackingEnabled(true) }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackerManager.stop()
        serviceScope.launch { (application as LongevityApp).preferences.setForegroundTrackingEnabled(false) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.step_tracking_active_title))
            .setContentText(getString(R.string.step_tracking_active_message))
            .setOngoing(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.step_tracking_channel_name), NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "steps_tracking"
        private const val NOTIFICATION_ID = 3202
    }
}
