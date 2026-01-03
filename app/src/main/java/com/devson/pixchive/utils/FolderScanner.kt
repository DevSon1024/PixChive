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
    // OPTIMIZATION: Increased to 500 to reduce DB transaction overhead and Flow emissions
    private const val BATCH_SIZE = 500
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

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
        val regex = Regex("(\\d+)|(\\D+)")
        val s1Parts = regex.findAll(s1).map { it.value }.toList()
        val s2Parts = regex.findAll(s2).map { it.value }.toList()

        val length = minOf(s1Parts.size, s2Parts.size)
        for (i in 0 until length) {
            val part1 = s1Parts[i]
            val part2 = s2Parts[i]

            val isDigit1 = part1[0].isDigit()
            val isDigit2 = part2[0].isDigit()

            if (isDigit1 && isDigit2) {
                val cmp = part1.toBigInteger().compareTo(part2.toBigInteger())
                if (cmp != 0) return cmp
            } else {
                val cmp = part1.compareTo(part2, ignoreCase = true)
                if (cmp != 0) return cmp
            }
        }
        return s1Parts.size - s2Parts.size
    }
}