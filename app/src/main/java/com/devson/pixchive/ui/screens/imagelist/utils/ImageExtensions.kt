package com.devson.pixchive.ui.screens.imagelist.utils

import android.net.Uri
import com.devson.pixchive.data.local.ImageEntity

/**
 * Common extension functions for ImageEntity objects and lists inside the ImageList screen.
 */

fun List<ImageEntity>.getUris(): List<Uri> {
    return this.mapNotNull {
        runCatching { Uri.parse(it.uri) }.getOrNull()
    }
}

// Optional: Helper to format size if needed locally
fun Long.formatAsFileSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$this B"
    }
}