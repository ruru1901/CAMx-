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
import com.pausecard.app.groq.GroqCardGenerator
import com.pausecard.app.overlay.CardOverlayManager
import com.pausecard.app.overlay.OverlayStateManager
import com.pausecard.app.util.CrashReporting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppWatcherService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var usageStatsManager: UsageStatsManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var isRunning = false
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0
    private var overlayManager: CardOverlayManager? = null
    private val cooldownMs = 20000L

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
            CrashReporting.logInfo(TAG, "Starting foreground service")
            isRunning = true
            startForegroundWithNotification()
            acquireWakeLock()
            startPolling()
            preFetchGroqCards()
            CrashReporting.logInfo(TAG, "Service fully started")
        } else {
            CrashReporting.logInfo(TAG, "onStartCommand called but already running")
        }

        return START_STICKY
    }

    private fun preFetchGroqCards() {
        serviceScope.launch {
            try {
                val generator = GroqCardGenerator(this@AppWatcherService)
                val today = java.time.LocalDate.now().toString()
                val lastDate = generator.getLastCallDate()
                val count = generator.getDailyCallCount()
                val db = com.pausecard.app.data.db.PauseCardDatabase.getInstance(this@AppWatcherService)
                val cardCount = db.cardDao().getCardCount()

                if (lastDate != today || count == 0) {
                    if (cardCount < 30) {
                        CrashReporting.logInfo(TAG, "Pre-fetching Groq cards (only $cardCount in DB)")
                        generator.generateCards()
                    }
                }
            } catch (e: Exception) {
                CrashReporting.logError(TAG, "Groq pre-fetch failed (non-critical)", e)
            }
        }
    }

    private fun startForegroundWithNotification() {
        try {
            val notification = NotificationCompat.Builder(this, PauseCardApp.CHANNEL_ID)
                .setContentTitle(getString(R.string.foreground_notification_title))
                .setContentText(getString(R.string.foreground_notification_text))
                .setSmallIcon(R.drawable.ic_notification_pause)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            CrashReporting.logInfo(TAG, "Foreground notification shown")
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "startForeground failed", e)
            // Try once more with a system icon as fallback
            try {
                val fallback = NotificationCompat.Builder(this, PauseCardApp.CHANNEL_ID)
                    .setContentTitle(getString(R.string.foreground_notification_title))
                    .setContentText(getString(R.string.foreground_notification_text))
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
                startForeground(NOTIFICATION_ID, fallback)
                CrashReporting.logInfo(TAG, "Foreground notification shown with fallback icon")
            } catch (e2: Exception) {
                CrashReporting.logError(TAG, "startForeground fallback also failed", e2)
            }
        }
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
            CrashReporting.logInfo(TAG, "Polling thread started (interval=${POLL_INTERVAL_MS}ms)")
            var lastSeenPackage: String? = null
            var lastSeenTime: Long = 0
            var pollCount = 0

            while (isRunning) {
                try {
                    pollCount++
                    checkForegroundApp { pkg, time ->
                        if (pkg != lastSeenPackage || (time - lastSeenTime) > 3000) {
                            lastSeenPackage = pkg
                            lastSeenTime = time
                            CrashReporting.logInfo(TAG, "Detected: $pkg at $time")
                            handleForegroundApp(pkg)
                        }
                    }
                } catch (e: Exception) {
                    CrashReporting.logError(TAG, "Polling error", e)
                }
                if (pollCount % 100 == 0) {
                    CrashReporting.logInfo(TAG, "Polling alive: ${pollCount} polls so far")
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

    private fun checkForegroundApp(onApp: (String, Long) -> Unit) {
        val usm = usageStatsManager
        if (usm == null) {
            CrashReporting.logWarning(TAG, "usageStatsManager is null")
            return
        }
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 5000, now)
        if (events == null) {
            CrashReporting.logWarning(TAG, "queryEvents returned null")
            return
        }

        val event = UsageEvents.Event()
        var topPackage: String? = null
        var topTime: Long = 0
        var eventCount = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            eventCount++
            CrashReporting.logInfo(TAG, "Event: pkg=${event.packageName} type=${event.eventType} time=${event.timeStamp}")
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (event.timeStamp >= topTime) {
                        topPackage = event.packageName
                        topTime = event.timeStamp
                    }
                }
            }
        }

        CrashReporting.logInfo(TAG, "Poll: ${eventCount} events found, top=$topPackage")
        if (topPackage != null && topPackage != packageName) {
            onApp(topPackage, topTime)
        }
    }

    private fun handleForegroundApp(pkg: String) {
        val now = System.currentTimeMillis()

        CrashReporting.logInfo(TAG, "handleForegroundApp: $pkg")

        if (OverlayStateManager.isOverlayShowing()) {
            CrashReporting.logInfo(TAG, "Skipped: overlay already showing")
            return
        }
        if (pkg == lastInterceptedPackage && (now - lastInterceptTime) < cooldownMs) {
            CrashReporting.logInfo(TAG, "Skipped: within cooldown")
            return
        }
        if (!isAppEnabled(pkg)) {
            CrashReporting.logInfo(TAG, "Skipped: $pkg not enabled in prefs")
            return
        }

        CrashReporting.logInfo(TAG, "INTERCEPTED: $pkg")
        lastInterceptedPackage = pkg
        lastInterceptTime = now

        val appName = getAppLabel(pkg)
        val shown = overlayManager?.showCard(pkg, appName)
        CrashReporting.logInfo(TAG, "showCard returned: $shown")
    }

    private fun isAppEnabled(packageName: String): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("app_enabled_$packageName", false)
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
        serviceScope.cancel()
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
        private const val POLL_INTERVAL_MS = 800L
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
