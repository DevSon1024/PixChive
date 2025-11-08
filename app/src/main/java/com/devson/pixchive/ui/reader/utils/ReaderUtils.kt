package com.devson.pixchive.ui.reader.utils

import android.net.Uri

fun urisMatch(uri1: String, uri2: String): Boolean {
    return try {
        val parsed1 = Uri.parse(uri1)
        val parsed2 = Uri.parse(uri2)

        parsed1.scheme == parsed2.scheme &&
                parsed1.authority == parsed2.authority &&
                parsed1.path == parsed2.path
    } catch (e: Exception) {
        uri1 == uri2
    }
}