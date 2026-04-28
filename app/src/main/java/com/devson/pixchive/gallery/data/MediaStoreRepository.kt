package com.devson.pixchive.gallery.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.devson.pixchive.gallery.data.models.GalleryFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {

    suspend fun getFolders(): List<GalleryFolder> = withContext(Dispatchers.IO) {
        val foldersMap = mutableMapOf<String, GalleryFolder>()
        
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        
        // Sorting by DATE_MODIFIED descending ensures the thumbnail is the most recent image
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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown Folder"
                
                val contentUri = ContentUris.withAppendedId(uri, id)

                // Group by bucketId in memory
                if (foldersMap.containsKey(bucketId)) {
                    val existing = foldersMap[bucketId]!!
                    foldersMap[bucketId] = existing.copy(imageCount = existing.imageCount + 1)
                } else {
                    foldersMap[bucketId] = GalleryFolder(
                        bucketId = bucketId,
                        folderName = bucketName,
                        thumbnailUri = contentUri,
                        imageCount = 1
                    )
                }
            }
        }
        
        // Return as a list, sorted alphabetically by folder name
        return@withContext foldersMap.values.sortedBy { it.folderName }
    }
}