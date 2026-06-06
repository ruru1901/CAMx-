package com.pausecard.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = context.getSharedPreferences("pausecard_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("service_enabled", true)) {
                AppWatcherService.start(context)
            }
        }
    }
}
