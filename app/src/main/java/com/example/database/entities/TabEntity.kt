package com.example.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val url: String,
    val isPrivate: Boolean = false,
    val isDesktopMode: Boolean = false,
    val position: Int = 0,
    val lastAccessed: Long = System.currentTimeMillis()
)
