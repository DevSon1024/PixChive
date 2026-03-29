package com.devson.pixchive.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("DELETE FROM images WHERE folderId = :folderId")
    suspend fun deleteFolderContent(folderId: String)

    // Returns a continuous stream of images for a specific folder
    @Query("SELECT * FROM images WHERE folderId = :folderId")
    fun getImagesFlow(folderId: String): Flow<List<ImageEntity>>

    @Query("SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY images.parentFolderPath ASC, images.name ASC")
    fun getFavoritesPagedNameAsc(): PagingSource<Int, ImageEntity>

    @Query("SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY images.parentFolderPath DESC, images.name DESC")
    fun getFavoritesPagedNameDesc(): PagingSource<Int, ImageEntity>

    @Query("SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY favorite_images.addedAt DESC")
    fun getFavoritesPagedDateNewest(): PagingSource<Int, ImageEntity>

    @Query("SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY favorite_images.addedAt ASC")
    fun getFavoritesPagedDateOldest(): PagingSource<Int, ImageEntity>

    /**
     * Fixed sort PagingSource (kept for compatibility).
     */
    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath ASC, name ASC")
    fun getImagesByFolderPaged(folderId: String): PagingSource<Int, ImageEntity>

    // --- Sort-specific typed PagingSource queries ---
    // Room requires separate @Query methods (or a RawQuery) for dynamic ORDER BY.
    // Using typed queries avoids the overhead of SupportSQLiteQuery construction per page load.

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath ASC, name ASC")
    fun getImagesPagedNameAsc(folderId: String): PagingSource<Int, ImageEntity>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath DESC, name DESC")
    fun getImagesPagedNameDesc(folderId: String): PagingSource<Int, ImageEntity>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY dateModified DESC")
    fun getImagesPagedDateNewest(folderId: String): PagingSource<Int, ImageEntity>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY dateModified ASC")
    fun getImagesPagedDateOldest(folderId: String): PagingSource<Int, ImageEntity>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY size ASC")
    fun getImagesPagedSizeAsc(folderId: String): PagingSource<Int, ImageEntity>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY size DESC")
    fun getImagesPagedSizeDesc(folderId: String): PagingSource<Int, ImageEntity>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY path ASC")
    fun getImagesPagedPathAsc(folderId: String): PagingSource<Int, ImageEntity>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY path DESC")
    fun getImagesPagedPathDesc(folderId: String): PagingSource<Int, ImageEntity>

    /**
     * Sort-aware PagingSource - ORDER BY is injected dynamically by the ViewModel
     * so the grid and the reader always use an identical sort order.
     */
    @RawQuery(observedEntities = [ImageEntity::class])
    fun getImagesByFolderPagedRaw(query: SupportSQLiteQuery): PagingSource<Int, ImageEntity>

    @Query("SELECT COUNT(*) FROM images WHERE folderId = :folderId")
    suspend fun getImageCount(folderId: String): Int

    @Query("SELECT COUNT(*) FROM images WHERE folderId = :folderId")
    fun getImageCountFlow(folderId: String): Flow<Int>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath ASC, name ASC LIMIT 1 OFFSET :index")
    suspend fun getImageByIndex(folderId: String, index: Int): ImageEntity?

    /**
     * Sort-aware single-image fetch by row offset - must use SAME ORDER BY as getImagesByFolderPagedRaw.
     */
    @RawQuery
    suspend fun getImageByIndexRaw(query: SupportSQLiteQuery): ImageEntity?

    @Query("SELECT COUNT(DISTINCT parentFolderPath) FROM images WHERE folderId = :folderId")
    suspend fun getChapterCount(folderId: String): Int

    @androidx.room.Query("SELECT path FROM images WHERE folderId = :folderId")
    suspend fun getAllPathsForFolder(folderId: String): List<String>

    @androidx.room.Query("DELETE FROM images WHERE path IN (:paths)")
    suspend fun deleteImagesByPaths(paths: List<String>)

}