package com.devson.pixchive.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("DELETE FROM images WHERE folderId = :folderId")
    suspend fun deleteFolderContent(folderId: String)

    // Returns a continuous stream of images for a specific folder
    @Query("SELECT * FROM images WHERE folderId = :folderId")
    fun getImagesFlow(folderId: String): Flow<List<ImageEntity>>

    @Query("SELECT COUNT(*) FROM images WHERE folderId = :folderId")
    suspend fun getImageCount(folderId: String): Int

    @Query("SELECT COUNT(DISTINCT parentFolderPath) FROM images WHERE folderId = :folderId")
    suspend fun getChapterCount(folderId: String): Int
}