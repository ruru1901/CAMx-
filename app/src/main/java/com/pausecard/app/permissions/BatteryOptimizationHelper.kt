package com.pausecard.app.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.pausecard.app.util.CrashReporting

object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimizationHelper"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            }
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Failed to request battery optimization ignore", e)
            fallbackToSettings(activity)
        }
    }

    private fun fallbackToSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            activity.startActivity(intent)
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Failed to open battery settings", e)
        }
    }
}
