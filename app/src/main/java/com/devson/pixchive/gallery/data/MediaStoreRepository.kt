package com.devson.pixchive.gallery.data

import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.devson.pixchive.gallery.data.models.GalleryFolder
import com.devson.pixchive.gallery.data.models.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreRepository(private val context: Context) {

    // --- PHASE 2 FUNCTION: Gets the folders ---
    suspend fun getFolders(): List<GalleryFolder> = withContext(Dispatchers.IO) {
        val foldersMap = mutableMapOf<String, GalleryFolder>()

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown Folder"
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                var realPath = cursor.getString(dataColumn) ?: ""
                
                // Fallback for Android 10+ if DATA is empty, restricted, or incorrectly returning a URI
                val relPathCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if ((realPath.isBlank() || realPath.startsWith("content://")) && relPathCol != -1 && nameCol != -1) {
                    val rel = cursor.getString(relPathCol) ?: ""
                    val name = cursor.getString(nameCol) ?: ""
                    if (rel.isNotBlank() && name.isNotBlank()) {
                        realPath = "/storage/emulated/0/$rel$name"
                    }
                }
                
                val folderPath = realPath.substringBeforeLast('/', "")

                val contentUri = ContentUris.withAppendedId(uri, id)

                if (foldersMap.containsKey(bucketId)) {
                    val existing = foldersMap[bucketId]!!
                    foldersMap[bucketId] = existing.copy(
                        imageCount = existing.imageCount + 1,
                        size = existing.size + size
                    )
                } else {
                    foldersMap[bucketId] = GalleryFolder(
                        bucketId = bucketId,
                        folderName = bucketName,
                        folderPath = folderPath,
                        thumbnailUri = contentUri,
                        imageCount = 1,
                        size = size,
                        dateModified = dateModified
                    )
                }
            }
        }

        return@withContext foldersMap.values.sortedBy { it.folderName }
    }
    suspend fun getImagesForFolder(bucketId: String): List<GalleryImage> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            // Optional columns for modern Android
            val relativePathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                var realPath = cursor.getString(dataColumn) ?: ""
                
                // Fallback for Android 10+ if DATA is empty, restricted, or incorrectly returning a URI
                if ((realPath.isBlank() || realPath.startsWith("content://")) && relativePathColumn != -1 && displayNameColumn != -1) {
                    val relPath = cursor.getString(relativePathColumn) ?: ""
                    val name = cursor.getString(displayNameColumn) ?: ""
                    if (relPath.isNotBlank() && name.isNotBlank()) {
                        realPath = "/storage/emulated/0/$relPath$name"
                    }
                }

                val dateModified = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(
                    GalleryImage(
                        id = id,
                        uri = contentUri,
                        realPath = realPath,
                        dateModified = dateModified,
                        size = size,
                        width = width,
                        height = height
                    )
                )
            }
        }
        return@withContext imageList
    }

    suspend fun getAllImages(): List<GalleryImage> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val relPathCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                var realPath = cursor.getString(dataCol) ?: ""

                // Fallback for Android 10+ if DATA is empty, restricted, or incorrectly returning a URI
                if ((realPath.isBlank() || realPath.startsWith("content://")) && relPathCol != -1 && nameCol != -1) {
                    val rel = cursor.getString(relPathCol) ?: ""
                    val name = cursor.getString(nameCol) ?: ""
                    if (rel.isNotBlank() && name.isNotBlank()) {
                        realPath = "/storage/emulated/0/$rel$name"
                    }
                }

                val dateAdded = cursor.getLong(dateAddedCol)
                val dateModified = cursor.getLong(dateModCol)
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(
                    GalleryImage(
                        id = id,
                        uri = contentUri,
                        realPath = realPath,
                        dateModified = dateModified,
                        dateAdded = dateAdded,
                        size = size,
                        width = width,
                        height = height
                    )
                )
            }
        }
        return@withContext imageList
    }

    suspend fun renameImage(id: Long, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newName)
            }
            context.contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFolder(folderPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldFolder = File(folderPath)
            val parent = oldFolder.parentFile
            val newFolder = File(parent, newName)
            
            if (oldFolder.renameTo(newFolder)) {
                // Trigger media scan to update MediaStore
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(newFolder.absolutePath),
                    null,
                    null
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}