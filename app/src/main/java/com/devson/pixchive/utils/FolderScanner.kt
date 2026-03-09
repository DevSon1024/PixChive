package com.devson.pixchive.utils

import android.net.Uri
import android.util.Log
import com.devson.pixchive.data.local.ImageDao
import com.devson.pixchive.data.local.ImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import kotlin.coroutines.coroutineContext

object FolderScanner {

    private const val TAG = "FolderScanner"
    private const val BATCH_SIZE = 500
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    // Define folders that should ALWAYS be ignored
    private val ignoredDirNames = setOf(
        ".thumbnails",
        ".trash",
        ".Trashed",
        ".trashed",
        ".Trash",
        "Trash",
        "\$RECYCLE.BIN",
        "lost+found"
    )

    /**
     * @param lastScanTime  Epoch-millis of the previous successful scan for this folder.
     *                      Pass **0L** (default) to force a full scan.
     *                      Any subdirectory whose [File.lastModified] is older than this
     *                      value will be skipped entirely - its images are assumed unchanged.
     */
    suspend fun scanAndInsert(
        folderUri: Uri,
        folderId: String,
        imageDao: ImageDao,
        showHidden: Boolean,
        lastScanTime: Long = 0L
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

            // 3. Recursive Scan (with cooperative cancellation via yield)
            //    lastScanTime is forwarded so unchanged subdirectories can be skipped.
            scanRecursive(rootFile, showHidden, lastScanTime) { file ->
                val entity = ImageEntity(
                    path = file.absolutePath,
                    folderId = folderId,
                    name = file.name,
                    size = file.length(),
                    formattedSize = formatFileSize(file.length()), // Pre-compute so Compose never runs Math.*
                    dateModified = file.lastModified(),
                    parentFolderPath = file.parentFile?.absolutePath ?: "",
                    parentFolderName = file.parentFile?.name ?: "",
                    uri = Uri.fromFile(file).toString()
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
        lastScanTime: Long,
        onImageFound: suspend (File) -> Unit
    ) {
        val files = directory.listFiles() ?: return

        for (file in files) {
            // Cooperative cancellation: yield() lets the dispatcher serve pending UI/other tasks
            // between each file during a deep scan, preventing thread starvation.
            yield()

            // Early-exit if the enclosing coroutine has been cancelled
            if (!coroutineContext.isActive) return

            // 1. Check for specific ignored folders (Thumbnails, Trash)
            if (file.isDirectory && isIgnoredDirectory(file.name)) {
                continue
            }

            // 2. Handle Hidden Files setting (skip if hidden AND showHidden is false)
            if (!showHidden && file.name.startsWith(".")) {
                continue
            }

            if (file.isDirectory) {
                // --- Delta-scan optimisation -----------------------------------
                // If this subdirectory (and everything beneath it) hasn't been
                // touched since the last scan we can skip the whole subtree.
                // lastModified on a directory is updated by the OS whenever a
                // direct child is added, removed, or renamed.
                // We use a small 1-second (1000 ms) grace-period to handle any
                // clock-skew between the filesystem and System.currentTimeMillis.
                if (lastScanTime > 0L && file.lastModified() < lastScanTime - 1_000L) {
                    Log.d(TAG, "Skipping unchanged dir: ${file.absolutePath}")
                    continue
                }
                // ---------------------------------------------------------------
                scanRecursive(file, showHidden, lastScanTime, onImageFound)
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

    /**
     * Converts a raw byte count into a human-readable string (e.g. "1.4 MB").
     * Called once per file at scan time and stored in [ImageEntity.formattedSize]
     * so that Compose composables never need to perform this calculation.
     */
    internal fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        return if (unitIndex == 0) "$bytes B"
        else String.format("%.1f %s", value, units[unitIndex])
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