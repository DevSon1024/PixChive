package com.devson.pixchive.utils

object PathUtils {
    /**
     * Extracts the folder name from a full path
     * Example: "Download/Ragalahari Downloads/Nayanthara/Nayanthara-1354" → "Nayanthara-1354"
     */
    fun extractFolderName(path: String): String {
        return path.substringAfterLast('/')
            .ifEmpty { path.substringAfterLast('\\') }
            .ifEmpty { path }
    }
}