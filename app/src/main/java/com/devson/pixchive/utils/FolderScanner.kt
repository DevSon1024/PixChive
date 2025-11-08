package com.devson.pixchive.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FolderScanner {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    /**
     * Scan folder and return list of chapters with images
     */
    suspend fun scanFolder(context: Context, folderUri: Uri): List<Chapter> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<Chapter>()

        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()

            // Get all subdirectories (chapters)
            val subFolders = folder.listFiles().filter { it.isDirectory }

            for (subFolder in subFolders) {
                val images = scanImagesInFolder(subFolder)
                if (images.isNotEmpty()) {
                    chapters.add(
                        Chapter(
                            name = subFolder.name ?: "Unknown",
                            path = subFolder.uri.toString(),
                            imageCount = images.size,
                            images = images
                        )
                    )
                }
            }

            // Sort chapters naturally (Chapter 1, Chapter 2, etc.)
            chapters.sortedWith(naturalOrderComparator())
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Scan all images recursively (for Flat View)
     */
    suspend fun scanAllImages(context: Context, folderUri: Uri): List<ImageFile> = withContext(Dispatchers.IO) {
        val allImages = mutableListOf<ImageFile>()

        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
            scanImagesRecursively(folder, allImages)
            allImages.sortedBy { it.name }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Scan images in a specific folder (non-recursive)
     */
    private fun scanImagesInFolder(folder: DocumentFile): List<ImageFile> {
        val images = mutableListOf<ImageFile>()

        folder.listFiles().forEach { file ->
            if (file.isFile && isImageFile(file.name ?: "")) {
                images.add(
                    ImageFile(
                        name = file.name ?: "Unknown",
                        path = file.uri.toString(),
                        uri = file.uri,
                        size = file.length()
                    )
                )
            }
        }

        return images.sortedBy { it.name }
    }

    /**
     * Recursively scan all images
     */
    private fun scanImagesRecursively(folder: DocumentFile, imageList: MutableList<ImageFile>) {
        folder.listFiles().forEach { file ->
            when {
                file.isFile && isImageFile(file.name ?: "") -> {
                    imageList.add(
                        ImageFile(
                            name = file.name ?: "Unknown",
                            path = file.uri.toString(),
                            uri = file.uri,
                            size = file.length()
                        )
                    )
                }
                file.isDirectory -> {
                    scanImagesRecursively(file, imageList)
                }
            }
        }
    }

    /**
     * Check if file is an image based on extension
     */
    private fun isImageFile(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in imageExtensions
    }

    /**
     * Natural order comparator for chapter names
     */
    private fun naturalOrderComparator(): Comparator<Chapter> {
        return Comparator { a, b ->
            val regex = Regex("(\\d+)")
            val aMatch = regex.find(a.name)
            val bMatch = regex.find(b.name)

            when {
                aMatch != null && bMatch != null -> {
                    val aNum = aMatch.value.toIntOrNull() ?: 0
                    val bNum = bMatch.value.toIntOrNull() ?: 0
                    aNum.compareTo(bNum)
                }
                else -> a.name.compareTo(b.name, ignoreCase = true)
            }
        }
    }

    /**
     * Count total images in folder
     */
    suspend fun countImages(context: Context, folderUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext 0
            countImagesRecursively(folder)
        } catch (e: Exception) {
            0
        }
    }

    private fun countImagesRecursively(folder: DocumentFile): Int {
        var count = 0
        folder.listFiles().forEach { file ->
            when {
                file.isFile && isImageFile(file.name ?: "") -> count++
                file.isDirectory -> count += countImagesRecursively(file)
            }
        }
        return count
    }
}