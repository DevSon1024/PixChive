package com.devson.pixchive.utils

import android.net.Uri
import android.util.Log
import com.devson.pixchive.data.local.ImageDao
import com.devson.pixchive.data.local.ImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FolderScanner {

    private const val TAG = "FolderScanner"
    private const val BATCH_SIZE = 500
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    // Define folders that should ALWAYS be ignored
    private val ignoredDirNames = setOf(
        ".thumbnails",
        ".trash",
        ".Trash",
        "Trash",
        "\$RECYCLE.BIN",
        "lost+found"
    )

    suspend fun scanAndInsert(
        folderUri: Uri,
        folderId: String,
        imageDao: ImageDao,
        showHidden: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val rootPath = getAbsolutePathFromSafUri(folderUri)
            if (rootPath == null) {
                Log.e(TAG, "Could not resolve path for URI: $folderUri")
                return@withContext
            }

            val rootFile = File(rootPath)
            if (!rootFile.exists() || !rootFile.isDirectory) {
                return@withContext
            }

            // 1. Clear previous data
            imageDao.deleteFolderContent(folderId)

            // 2. Buffer for batch insertion
            val buffer = mutableListOf<ImageEntity>()

            // 3. Recursive Scan
            scanRecursive(rootFile, showHidden) { file ->
                val entity = ImageEntity(
                    path = file.absolutePath,
                    folderId = folderId,
                    name = file.name,
                    size = file.length(),
                    dateModified = file.lastModified(),
                    parentFolderPath = file.parentFile?.absolutePath ?: "",
                    parentFolderName = file.parentFile?.name ?: ""
                )
                buffer.add(entity)

                if (buffer.size >= BATCH_SIZE) {
                    imageDao.insertImages(buffer.toList())
                    buffer.clear()
                }
            }

            // 4. Flush remaining items
            if (buffer.isNotEmpty()) {
                imageDao.insertImages(buffer)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder", e)
            e.printStackTrace()
        }
    }

    private suspend fun scanRecursive(
        directory: File,
        showHidden: Boolean,
        onImageFound: suspend (File) -> Unit
    ) {
        val files = directory.listFiles() ?: return

        for (file in files) {
            // 1. Check for specific ignored folders (Thumbnails, Trash)
            if (file.isDirectory && isIgnoredDirectory(file.name)) {
                continue
            }

            // 2. Handle Hidden Files setting (skip if hidden AND showHidden is false)
            if (!showHidden && file.name.startsWith(".")) {
                continue
            }

            if (file.isDirectory) {
                scanRecursive(file, showHidden, onImageFound)
            } else if (file.isFile && isImageFile(file.name)) {
                onImageFound(file)
            }
        }
    }

    private fun isIgnoredDirectory(name: String): Boolean {
        // Exact matches
        if (name in ignoredDirNames) return true

        // Pattern matches (e.g., .Trash-1000)
        if (name.startsWith(".trash", ignoreCase = true)) return true

        return false
    }

    private fun getAbsolutePathFromSafUri(uri: Uri): String? {
        try {
            val path = Uri.decode(uri.toString())
            if (path.contains(":")) {
                val id = path.substringAfterLast(":")
                if (path.contains("primary") || !path.contains("/tree/")) {
                    return "/storage/emulated/0/$id"
                }
                val volumeId = path.substringAfter("/tree/").substringBefore(":")
                return "/storage/$volumeId/$id"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun isImageFile(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in imageExtensions
    }

    fun compareNatural(s1: String, s2: String): Int {
        var i1 = 0
        var i2 = 0

        while (i1 < s1.length && i2 < s2.length) {
            val isDigit1 = s1[i1].isDigit()
            val isDigit2 = s2[i2].isDigit()

            if (isDigit1 && isDigit2) {
                // Find contiguous digits
                var j1 = i1
                while (j1 < s1.length && s1[j1].isDigit()) j1++
                var j2 = i2
                while (j2 < s2.length && s2[j2].isDigit()) j2++

                // Compare numeric ranges mathematically using lengths or custom trailing big integers
                val segment1 = s1.substring(i1, j1)
                val segment2 = s2.substring(i2, j2)
                
                val cmp = segment1.toBigDecimalOrNull()?.compareTo(segment2.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO)
                    ?: segment1.compareTo(segment2)

                if (cmp != 0) return cmp
                i1 = j1
                i2 = j2
            } else if (!isDigit1 && !isDigit2) {
                // Find contiguous non-digits
                var j1 = i1
                while (j1 < s1.length && !s1[j1].isDigit()) j1++
                var j2 = i2
                while (j2 < s2.length && !s2[j2].isDigit()) j2++

                val segment1 = s1.substring(i1, j1)
                val segment2 = s2.substring(i2, j2)

                val cmp = segment1.compareTo(segment2, ignoreCase = true)
                if (cmp != 0) return cmp
                i1 = j1
                i2 = j2
            } else {
                // One is digit, one is not
                val cmp = s1[i1].toString().compareTo(s2[i2].toString(), ignoreCase = true)
                if (cmp != 0) return cmp
                i1++
                i2++
            }
        }
        return s1.length - s2.length
    }
}