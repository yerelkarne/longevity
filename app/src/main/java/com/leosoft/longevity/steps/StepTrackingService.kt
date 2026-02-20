package com.leosoft.longevity.steps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.leosoft.longevity.LongevityApp
import com.leosoft.longevity.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class StepTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tracker: PedometerTracker? = null

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        val app = application as LongevityApp
        tracker = PedometerTracker(this, app.repository, app.preferences, serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        tracker?.startTracking(forceFallback = false)
        return START_STICKY
    }

    override fun onDestroy() {
        tracker?.stopTracking()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.step_tracking_active_title))
            .setContentText(getString(R.string.step_tracking_active_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Step Tracking", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "step_tracking"
        private const val NOTIFICATION_ID = 2001
    }
}
