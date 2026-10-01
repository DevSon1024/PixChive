package com.devson.pixchive.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {

    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

    private val dateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        }
    }

    private val dateTimeFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        }
    }

    private val monthDayFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMMM d", Locale.getDefault())
        }
    }

    private val monthYearFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        }
    }

    /**
     * Formats bytes into a clean, human-readable file size string.
     * Examples: 0 B, 500 B, 145 KB, 145 MB, 1.2 GB
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < 1024L) return "$bytes B"

        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < UNITS.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        val formatted = String.format(Locale.US, "%.1f", value)
        val trimmed = if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
        return "$trimmed ${UNITS[unitIndex]}"
    }

    /**
     * Thread-safe date formatting without allocating a new SimpleDateFormat instance per invocation.
     * Supports both millisecond and second timestamps automatically.
     */
    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val tsSeconds = if (timestamp > 100_000_000_000L) timestamp / 1000L else timestamp
        val formatter = dateFormatThreadLocal.get() ?: SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(tsSeconds * 1000L))
    }

    /**
     * Thread-safe date-time formatting without allocating a new SimpleDateFormat instance per invocation.
     * Supports both millisecond and second timestamps automatically.
     */
    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val tsSeconds = if (timestamp > 100_000_000_000L) timestamp / 1000L else timestamp
        val formatter = dateTimeFormatThreadLocal.get() ?: SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(tsSeconds * 1000L))
    }

    /**
     * High performance sticky date header label computation.
     * Avoids excessive Calendar and SimpleDateFormat allocations in Paging 3 separator pipelines.
     */
    fun getDateHeaderLabel(timestampSeconds: Long): String {
        if (timestampSeconds <= 0L) return "Undated"
        val tsMillis = timestampSeconds * 1000L

        val calNow = Calendar.getInstance()
        val currentYear = calNow.get(Calendar.YEAR)
        val currentDayOfYear = calNow.get(Calendar.DAY_OF_YEAR)

        val calTarget = Calendar.getInstance().apply { timeInMillis = tsMillis }
        val targetYear = calTarget.get(Calendar.YEAR)
        val targetDayOfYear = calTarget.get(Calendar.DAY_OF_YEAR)

        return when {
            targetYear == currentYear && targetDayOfYear == currentDayOfYear -> "Today"
            targetYear == currentYear && targetDayOfYear == currentDayOfYear - 1 -> "Yesterday"
            // Handle new year boundary for yesterday (e.g. Dec 31 vs Jan 1)
            targetYear == currentYear - 1 && currentDayOfYear == 1 && calTarget.get(Calendar.MONTH) == Calendar.DECEMBER && calTarget.get(Calendar.DAY_OF_MONTH) == 31 -> "Yesterday"
            targetYear == currentYear -> {
                val fmt = monthDayFormatThreadLocal.get() ?: SimpleDateFormat("MMMM d", Locale.getDefault())
                fmt.format(Date(tsMillis))
            }
            else -> {
                val fmt = monthYearFormatThreadLocal.get() ?: SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                fmt.format(Date(tsMillis))
            }
        }
    }
}
