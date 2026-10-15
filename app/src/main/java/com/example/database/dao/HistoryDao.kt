package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitCount DESC, timestamp DESC LIMIT :limit")
    fun getFrequentlyVisited(limit: Int = 8): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteHistoryByIds(ids: List<Long>)

    @Query("DELETE FROM history WHERE timestamp >= :sinceTimestamp")
    suspend fun deleteHistorySince(sinceTimestamp: Long)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}
