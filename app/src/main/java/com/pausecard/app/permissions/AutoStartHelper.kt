package com.pausecard.app.permissions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.pausecard.app.util.CrashReporting

object AutoStartHelper {

    private const val TAG = "AutoStartHelper"

    fun requestAutoStart(activity: Activity) {
        val manufacturer = Build.MANUFACTURER.lowercase()

        when {
            manufacturer.contains("xiaomi") -> requestAutoStartXiaomi(activity)
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> requestAutoStartOppo(activity)
            manufacturer.contains("huawei") -> requestAutoStartHuawei(activity)
            manufacturer.contains("samsung") -> requestAutoStartSamsung(activity)
            manufacturer.contains("vivo") -> requestAutoStartVivo(activity)
            else -> openBatterySettings(activity)
        }
    }

    private fun requestAutoStartXiaomi(activity: Activity) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            CrashReporting.logWarning(TAG, "Xiaomi auto-start intent failed, trying alternative")
            tryAlternativeXiaomi(activity)
        }
    }

    private fun tryAlternativeXiaomi(activity: Activity) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.powercenter.PowerSettings"
                )
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Xiaomi alternative auto-start failed", e)
            openBatterySettings(activity)
        }
    }

    private fun requestAutoStartOppo(activity: Activity) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            tryAlternativeOppo(activity)
        }
    }

    private fun tryAlternativeOppo(activity: Activity) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                )
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Oppo auto-start failed", e)
            openBatterySettings(activity)
        }
    }

    private fun requestAutoStartHuawei(activity: Activity) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Huawei auto-start failed", e)
            openBatterySettings(activity)
        }
    }

    private fun requestAutoStartSamsung(activity: Activity) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Samsung auto-start failed", e)
        }
    }

    private fun requestAutoStartVivo(activity: Activity) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Vivo auto-start failed", e)
            openBatterySettings(activity)
        }
    }

    private fun openBatterySettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            activity.startActivity(intent)
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Failed to open any battery settings", e)
        }
    }

    fun isAutoStartAvailable(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("vivo")
    }
}
