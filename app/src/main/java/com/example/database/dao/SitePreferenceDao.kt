package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entities.SitePreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SitePreferenceDao {
    @Query("SELECT * FROM site_preferences WHERE domain = :domain")
    fun getSitePreference(domain: String): Flow<SitePreferenceEntity?>

    @Query("SELECT * FROM site_preferences WHERE domain = :domain")
    suspend fun getSitePreferenceSync(domain: String): SitePreferenceEntity?

    @Query("SELECT * FROM site_preferences")
    fun getAllSitePreferences(): Flow<List<SitePreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preference: SitePreferenceEntity)

    @Query("DELETE FROM site_preferences WHERE domain = :domain")
    suspend fun deleteSitePreference(domain: String)
}
