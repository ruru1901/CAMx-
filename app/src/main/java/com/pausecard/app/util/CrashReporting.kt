package com.pausecard.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporting {

    private const val TAG = "PauseCard"
    private const val LOG_DIR = "crash_logs"
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) logDir.mkdirs()
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeToFile("ERROR", tag, message, throwable)
    }

    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
        writeToFile("WARN", tag, message, null)
    }

    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
        writeToFile("INFO", tag, message, null)
    }

    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable?) {
        try {
            val ctx = context ?: return
            val logDir = File(ctx.filesDir, LOG_DIR)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val logFile = File(logDir, "pausecard_${dateFormat.format(Date())}.log")

            val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
            val stackTrace = throwable?.let {
                val sw = StringWriter()
                it.printStackTrace(PrintWriter(sw))
                sw.toString()
            } ?: ""

            val logEntry = buildString {
                append("[${timeFormat.format(Date())}] $level/$tag: $message\n")
                if (stackTrace.isNotEmpty()) {
                    append("Stacktrace: $stackTrace\n")
                }
                append("---\n")
            }

            logFile.appendText(logEntry)
            trimOldLogs(logDir)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    private fun trimOldLogs(logDir: File) {
        val files = logDir.listFiles()?.sortedByDescending { it.name } ?: return
        if (files.size > 7) {
            files.drop(7).forEach { it.delete() }
        }
    }
}
