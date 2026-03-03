package com.devson.pixchive.data.local

import androidx.paging.PagingSource
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

    /**
     * Returns a [PagingSource] for Paging 3. Room invalidates it automatically
     * whenever rows for this folderId change, so the UI updates live during scanning.
     */
    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath ASC, name ASC")
    fun getImagesByFolderPaged(folderId: String): PagingSource<Int, ImageEntity>

    @Query("SELECT COUNT(*) FROM images WHERE folderId = :folderId")
    suspend fun getImageCount(folderId: String): Int

    @Query("SELECT COUNT(*) FROM images WHERE folderId = :folderId")
    fun getImageCountFlow(folderId: String): Flow<Int>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath ASC, name ASC LIMIT 1 OFFSET :index")
    suspend fun getImageByIndex(folderId: String, index: Int): ImageEntity?

    @Query("SELECT COUNT(DISTINCT parentFolderPath) FROM images WHERE folderId = :folderId")
    suspend fun getChapterCount(folderId: String): Int
}