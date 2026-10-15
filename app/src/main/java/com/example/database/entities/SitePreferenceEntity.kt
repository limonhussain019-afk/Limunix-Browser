package com.example.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_preferences")
data class SitePreferenceEntity(
    @PrimaryKey
    val domain: String,
    val adBlockEnabled: Boolean = true,
    val trackerBlockEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true
)
