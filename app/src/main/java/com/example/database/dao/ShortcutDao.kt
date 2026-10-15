package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entities.ShortcutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY isDefault DESC, id ASC")
    fun getAllShortcuts(): Flow<List<ShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultShortcuts(shortcuts: List<ShortcutEntity>)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteShortcutById(id: Long)

    @Query("SELECT COUNT(*) FROM shortcuts")
    suspend fun getCount(): Int
}
