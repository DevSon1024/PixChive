package com.devson.pixchive.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("DELETE FROM images WHERE folderId = :folderId")
    suspend fun deleteFolderContent(folderId: String)

    @Query("SELECT * FROM images WHERE folderId = :folderId")
    fun getImagesFlow(folderId: String): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY dateModified DESC LIMIT 1")
    fun getLatestImageFlow(folderId: String): Flow<ImageEntity?>

    // Chunked non-Paging fetch to avoid loading the entire table in one allocation.
    // Use limit/offset loops on the call site to stream results incrementally.
    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath ASC, name ASC LIMIT :limit OFFSET :offset")
    suspend fun getImagesChunked(folderId: String, limit: Int, offset: Int): List<ImageEntity>

    @Query("SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY images.parentFolderPath ASC, images.name ASC")
    fun getFavoritesPagedNameAsc(): PagingSource<Int, ImageEntity>

    @Query("SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY images.parentFolderPath DESC, images.name DESC")
    fun getFavoritesPagedNameDesc(): PagingSource<Int, ImageEntity>

    @Query("SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY favorite_images.addedAt DESC")
    fun getFavoritesPagedDateNewest(): PagingSource<Int, ImageEntity>

    @Query("SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY favorite_images.addedAt ASC")
    fun getFavoritesPagedDateOldest(): PagingSource<Int, ImageEntity>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath ASC, name ASC")
    fun getImagesByFolderPaged(folderId: String): PagingSource<Int, ImageEntity>

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

    @RawQuery(observedEntities = [ImageEntity::class])
    fun getImagesByFolderPagedRaw(query: SupportSQLiteQuery): PagingSource<Int, ImageEntity>

    @Query("SELECT COUNT(*) FROM images WHERE folderId = :folderId")
    suspend fun getImageCount(folderId: String): Int

    @Query("SELECT COUNT(*) FROM images WHERE folderId = :folderId")
    fun getImageCountFlow(folderId: String): Flow<Int>

    @Query("SELECT * FROM images WHERE folderId = :folderId ORDER BY parentFolderPath ASC, name ASC LIMIT 1 OFFSET :index")
    suspend fun getImageByIndex(folderId: String, index: Int): ImageEntity?

    @RawQuery
    suspend fun getImageByIndexRaw(query: SupportSQLiteQuery): ImageEntity?

    @Query("SELECT COUNT(DISTINCT parentFolderPath) FROM images WHERE folderId = :folderId")
    suspend fun getChapterCount(folderId: String): Int

    @Query("SELECT path FROM images WHERE folderId = :folderId")
    suspend fun getAllPathsForFolder(folderId: String): List<String>

    // Wrapped in @Transaction so the entire batch delete is one atomic DB write,
    // preventing partial deletes that would leave the DB in an inconsistent state
    // and also releasing the write lock in a single commit instead of N commits.
    @Transaction
    suspend fun deleteImagesByPathsBatched(paths: Collection<String>) {
        paths.chunked(900).forEach { chunk ->
            deleteImagesByPaths(chunk)
        }
    }

    @Query("DELETE FROM images WHERE path IN (:paths)")
    suspend fun deleteImagesByPaths(paths: List<String>)
}