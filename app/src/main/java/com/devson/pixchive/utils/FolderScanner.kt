package com.devson.pixchive.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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
    // Smaller batch = shorter write locks, less heap pressure per commit
    private const val BATCH_SIZE = 500
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    private val ignoredDirNames = setOf(
        ".thumbnails", ".trash", ".Trashed", ".trashed", ".Trash", "Trash",
        "\$RECYCLE.BIN", "lost+found"
    )

    /**
     * @param lastScanTime Epoch-millis of the previous successful scan. Pass 0L for a full scan.
     */
    suspend fun scanAndInsert(
        context: Context,
        folderUri: Uri,
        folderId: String,
        imageDao: ImageDao,
        showHidden: Boolean,
        lastScanTime: Long = 0L
    ) = withContext(Dispatchers.IO) {
        // Build MediaStore URI map strictly on IO; cooperative-yield every 5000 rows
        // so a huge MediaStore doesn't block cancellation for too long.
        val mediaStoreUriMap = buildMediaStoreUriMap(context)

        try {
            val rootPath = getAbsolutePathFromSafUri(folderUri)
            if (rootPath == null) {
                Log.e(TAG, "Could not resolve path for URI: $folderUri")
                return@withContext
            }

            val rootFile = File(rootPath)
            if (!rootFile.exists() || !rootFile.isDirectory) return@withContext

            val existingPaths = imageDao.getAllPathsForFolder(folderId).toMutableSet()
            val buffer = mutableListOf<ImageEntity>()

            scanRecursive(rootFile, showHidden, lastScanTime, existingPaths) { file ->
                val contentUri = mediaStoreUriMap[file.absolutePath]
                    ?: Uri.fromFile(file).toString()

                buffer.add(
                    ImageEntity(
                        path = file.absolutePath,
                        folderId = folderId,
                        name = file.name,
                        size = file.length(),
                        formattedSize = formatFileSize(file.length()),
                        dateModified = file.lastModified(),
                        parentFolderPath = file.parentFile?.absolutePath ?: "",
                        parentFolderName = file.parentFile?.name ?: "",
                        uri = contentUri
                    )
                )
                existingPaths.remove(file.absolutePath)

                if (buffer.size >= BATCH_SIZE) {
                    // Each batch is its own transaction; keeps write-lock windows short
                    imageDao.insertImages(buffer.toList())
                    buffer.clear()
                    // Yield after each DB commit so cancellation is honoured promptly
                    yield()
                }
            }

            if (buffer.isNotEmpty()) {
                imageDao.insertImages(buffer)
                buffer.clear()
            }

            // Single atomic delete of all stale paths using the DAO's @Transaction helper
            if (existingPaths.isNotEmpty()) {
                imageDao.deleteImagesByPathsBatched(existingPaths)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder", e)
        }
    }

    /**
     * Queries MediaStore on IO with cooperative cancellation every 5000 rows.
     */
    private suspend fun buildMediaStoreUriMap(context: Context): Map<String, String> =
        withContext(Dispatchers.IO) {
            val map = mutableMapOf<String, String>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
            )
            try {
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    var rowCount = 0
                    while (cursor.moveToNext()) {
                        if (!coroutineContext.isActive) break
                        // Yield every 5000 rows to stay cancellation-friendly
                        if (++rowCount % 5000 == 0) yield()
                        val id = cursor.getLong(idCol)
                        val data = cursor.getString(dataCol) ?: continue
                        map[data] = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()
                        ).toString()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore query failed, falling back to file:// URIs", e)
            }
            map
        }

    private suspend fun scanRecursive(
        directory: File,
        showHidden: Boolean,
        lastScanTime: Long,
        existingPaths: MutableSet<String>,
        onImageFound: suspend (File) -> Unit
    ) {
        val files = directory.listFiles() ?: return

        for (file in files) {
            yield()
            if (!coroutineContext.isActive) return

            if (file.isDirectory && isIgnoredDirectory(file.name)) continue
            if (!showHidden && file.name.startsWith(".")) continue

            if (file.isDirectory) {
                if (lastScanTime > 0L && file.lastModified() < lastScanTime - 1_000L) {
                    Log.d(TAG, "Skipping unchanged dir: ${file.absolutePath}")
                    existingPaths.removeAll { it.startsWith(file.absolutePath + File.separator) }
                    continue
                }
                scanRecursive(file, showHidden, lastScanTime, existingPaths, onImageFound)
            } else if (file.isFile && isImageFile(file.name)) {
                onImageFound(file)
            }
        }
    }

    private fun isIgnoredDirectory(name: String): Boolean {
        if (name in ignoredDirNames) return true
        if (name.startsWith(".trash", ignoreCase = true)) return true
        return false
    }

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
                var j1 = i1
                while (j1 < s1.length && s1[j1].isDigit()) j1++
                var j2 = i2
                while (j2 < s2.length && s2[j2].isDigit()) j2++

                val segment1 = s1.substring(i1, j1)
                val segment2 = s2.substring(i2, j2)

                val cmp = segment1.toBigDecimalOrNull()
                    ?.compareTo(segment2.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO)
                    ?: segment1.compareTo(segment2)

                if (cmp != 0) return cmp
                i1 = j1
                i2 = j2
            } else if (!isDigit1 && !isDigit2) {
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
                val cmp = s1[i1].toString().compareTo(s2[i2].toString(), ignoreCase = true)
                if (cmp != 0) return cmp
                i1++
                i2++
            }
        }
        return s1.length - s2.length
    }
}