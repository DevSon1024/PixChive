package com.devson.pixchive.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY lastAccessed DESC LIMIT 10")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE chapterPath = :chapterPath")
    suspend fun delete(chapterPath: String)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
