package com.devson.pixchive.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun init(context: Context) {
        logFile = File(context.filesDir, "app_logs.txt")
        if (logFile?.exists() == false) {
            logFile?.createNewFile()
        }

        // Intercept global crashes and save them
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("FATAL_CRASH", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        writeLog("ERROR", tag, message, throwable)
    }

    fun w(tag: String, message: String) {
        writeLog("WARN", tag, message, null)
    }

    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable?) {
        val time = dateFormat.format(Date())
        val trace = throwable?.stackTraceToString()?.let { "\n$it" } ?: ""
        val logMessage = "[$time] [$level] $tag: $message$trace\n"
        try {
            logFile?.appendText(logMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogsLast24Hours(): List<String> {
        val file = logFile ?: return emptyList()
        if (!file.exists()) return emptyList()

        // Split logs by the timestamp bracket [YYYY-MM-DD so stack traces stay attached to their parent log
        val allLogs = file.readText().split("\n(?=\\[\\d{4}-\\d{2}-\\d{2})".toRegex()).filter { it.isNotBlank() }
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        return allLogs.filter { logLine ->
            try {
                // Extract "yyyy-MM-dd HH:mm:ss" from "[yyyy-MM-dd HH:mm:ss] [LEVEL] ..."
                val dateStr = logLine.substringAfter("[").substringBefore("]")
                val logDate = dateFormat.parse(dateStr)
                logDate != null && logDate.time >= twentyFourHoursAgo
            } catch (e: Exception) {
                false
            }
        }.reversed() // Show newest first
    }

    fun clearLogs() {
        logFile?.writeText("")
    }
}