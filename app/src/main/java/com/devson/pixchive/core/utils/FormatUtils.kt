package com.devson.pixchive.core.utils

import java.util.Locale

object FormatUtils {
    /**
     * Formats bytes into a clean, human-readable file size string.
     * Examples: 0 B, 500 B, 145 KB, 145 MB, 1.2 GB
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        return if (unitIndex == 0) {
            "$bytes B"
        } else {
            val formatted = String.format(Locale.US, "%.1f", value)
            val trimmed = if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
            "$trimmed ${units[unitIndex]}"
        }
    }
}
