package com.pausecard.app.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.app.Service
import androidx.core.app.NotificationCompat
import com.pausecard.app.PauseCardApp
import com.pausecard.app.R
import com.pausecard.app.overlay.CardOverlayManager
import com.pausecard.app.overlay.OverlayStateManager
import com.pausecard.app.util.CrashReporting

class AppWatcherService : Service() {

    private var usageStatsManager: UsageStatsManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var isRunning = false
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0
    private var overlayManager: CardOverlayManager? = null
    private val cooldownMs = 15000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        overlayManager = CardOverlayManager(this)
        CrashReporting.logInfo(TAG, "AppWatcherService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!isRunning) {
            isRunning = true
            startForegroundWithNotification()
            acquireWakeLock()
            startPolling()
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = NotificationCompat.Builder(this, PauseCardApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PauseCard::WatcherWakeLock"
        )?.apply {
            acquire(60 * 60 * 1000L)
        }
    }

    private fun startPolling() {
        Thread({
            while (isRunning) {
                try {
                    checkForegroundApp()
                } catch (e: Exception) {
                    CrashReporting.logError(TAG, "Polling error", e)
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS)
                } catch (e: InterruptedException) {
                    break
                }
            }
            CrashReporting.logInfo(TAG, "Polling thread ended")
        }, "AppWatcher-Poll").apply {
            isDaemon = true
            start()
        }
    }

    private fun checkForegroundApp() {
        val usm = usageStatsManager ?: return
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 2000, now)

        val event = UsageEvents.Event()
        var topPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                topPackage = event.packageName
            }
        }

        if (topPackage != null && topPackage != packageName) {
            handleForegroundApp(topPackage)
        }
    }

    private fun handleForegroundApp(pkg: String) {
        val now = System.currentTimeMillis()

        if (OverlayStateManager.isOverlayShowing()) return
        if (pkg == lastInterceptedPackage && (now - lastInterceptTime) < cooldownMs) return
        if (!isAppEnabled(pkg)) return

        CrashReporting.logInfo(TAG, "INTERCEPTED: $pkg")
        lastInterceptedPackage = pkg
        lastInterceptTime = now

        val appName = getAppLabel(pkg)
        overlayManager?.showCard(pkg, appName)
    }

    private fun isAppEnabled(packageName: String): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("app_enabled_$packageName", true)
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
                .replaceFirstChar { it.uppercase() }
        }
    }

    override fun onDestroy() {
        isRunning = false
        overlayManager?.cleanup()
        overlayManager = null
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        CrashReporting.logInfo(TAG, "AppWatcherService destroyed")
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val prefs = getSharedPreferences("pausecard_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("service_enabled", true)) {
            CrashReporting.logInfo(TAG, "Task removed, restarting service")
            val restartIntent = Intent(applicationContext, AppWatcherService::class.java)
            startForegroundService(restartIntent)
        }
    }

    companion object {
        private const val TAG = "AppWatcherService"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 1200L
        private const val PREFS_NAME = "pausecard_prefs"
        const val ACTION_STOP = "com.pausecard.app.STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, AppWatcherService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppWatcherService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
