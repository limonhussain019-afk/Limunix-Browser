package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.database.dao.BookmarkDao
import com.example.database.dao.DownloadDao
import com.example.database.dao.HistoryDao
import com.example.database.dao.ShortcutDao
import com.example.database.dao.SitePreferenceDao
import com.example.database.dao.TabDao
import com.example.database.entities.BookmarkEntity
import com.example.database.entities.DownloadEntity
import com.example.database.entities.HistoryEntity
import com.example.database.entities.ShortcutEntity
import com.example.database.entities.SitePreferenceEntity
import com.example.database.entities.TabEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        HistoryEntity::class,
        BookmarkEntity::class,
        DownloadEntity::class,
        TabEntity::class,
        SitePreferenceEntity::class,
        ShortcutEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LimunixDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadDao(): DownloadDao
    abstract fun tabDao(): TabDao
    abstract fun sitePreferenceDao(): SitePreferenceDao
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: LimunixDatabase? = null

        fun getDatabase(context: Context): LimunixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LimunixDatabase::class.java,
                    "limunix_browser.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default shortcuts
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.shortcutDao()?.insertDefaultShortcuts(
                                    listOf(
                                        ShortcutEntity(title = "DuckDuckGo", url = "https://duckduckgo.com", iconKey = "search", isDefault = true),
                                        ShortcutEntity(title = "Wikipedia", url = "https://en.wikipedia.org", iconKey = "book", isDefault = true),
                                        ShortcutEntity(title = "GitHub", url = "https://github.com", iconKey = "code", isDefault = true),
                                        ShortcutEntity(title = "Reddit", url = "https://www.reddit.com", iconKey = "forum", isDefault = true),
                                        ShortcutEntity(title = "Hacker News", url = "https://news.ycombinator.com", iconKey = "trending_up", isDefault = true),
                                        ShortcutEntity(title = "YouTube", url = "https://www.youtube.com", iconKey = "play_arrow", isDefault = true),
                                        ShortcutEntity(title = "BBC News", url = "https://www.bbc.com", iconKey = "public", isDefault = true),
                                        ShortcutEntity(title = "MDN Web Docs", url = "https://developer.mozilla.org", iconKey = "terminal", isDefault = true)
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
