package com.devson.pixchive.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite_images WHERE uri = :uri")
    suspend fun delete(uri: String)

    @Query("SELECT * FROM favorite_images WHERE uri = :uri")
    suspend fun getFavorite(uri: String): FavoriteEntity?

    @Query("SELECT uri FROM favorite_images")
    fun getAllFavoriteUrisFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringConflicts(favorites: List<FavoriteEntity>)
}
