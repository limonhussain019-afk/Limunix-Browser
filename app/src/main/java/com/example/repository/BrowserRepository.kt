package com.example.repository

import com.example.database.LimunixDatabase
import com.example.database.entities.BookmarkEntity
import com.example.database.entities.DownloadEntity
import com.example.database.entities.DownloadStatus
import com.example.database.entities.HistoryEntity
import com.example.database.entities.ShortcutEntity
import com.example.database.entities.SitePreferenceEntity
import com.example.database.entities.TabEntity
import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val database: LimunixDatabase) {

    // History
    val allHistory: Flow<List<HistoryEntity>> = database.historyDao().getAllHistory()
    val frequentlyVisited: Flow<List<HistoryEntity>> = database.historyDao().getFrequentlyVisited()

    fun searchHistory(query: String): Flow<List<HistoryEntity>> = database.historyDao().searchHistory(query)

    suspend fun addHistory(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("chrome:") || url.startsWith("data:")) return
        database.historyDao().insertHistory(
            HistoryEntity(
                url = url,
                title = if (title.isBlank()) url else title,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) = database.historyDao().deleteHistoryById(id)

    suspend fun deleteHistoryItems(ids: List<Long>) = database.historyDao().deleteHistoryByIds(ids)

    suspend fun deleteHistorySince(timestamp: Long) = database.historyDao().deleteHistorySince(timestamp)

    suspend fun clearAllHistory() = database.historyDao().clearAllHistory()

    // Bookmarks
    val allBookmarks: Flow<List<BookmarkEntity>> = database.bookmarkDao().getAllBookmarks()
    val allFolders: Flow<List<String>> = database.bookmarkDao().getAllFolders()

    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> = database.bookmarkDao().searchBookmarks(query)

    fun isBookmarked(url: String): Flow<Boolean> = database.bookmarkDao().isBookmarked(url)

    suspend fun addBookmark(url: String, title: String, folder: String = "Mobile Bookmarks") {
        database.bookmarkDao().insertBookmark(
            BookmarkEntity(
                url = url,
                title = if (title.isBlank()) url else title,
                folder = folder
            )
        )
    }

    suspend fun updateBookmark(bookmark: BookmarkEntity) = database.bookmarkDao().updateBookmark(bookmark)

    suspend fun deleteBookmark(id: Long) = database.bookmarkDao().deleteBookmarkById(id)

    suspend fun deleteBookmarkByUrl(url: String) = database.bookmarkDao().deleteBookmarkByUrl(url)

    // Downloads
    val allDownloads: Flow<List<DownloadEntity>> = database.downloadDao().getAllDownloads()

    suspend fun getDownload(id: String): DownloadEntity? = database.downloadDao().getDownloadById(id)

    suspend fun insertDownload(download: DownloadEntity) = database.downloadDao().insertDownload(download)

    suspend fun updateDownloadProgress(id: String, downloaded: Long, speed: Long, eta: Long, status: DownloadStatus) =
        database.downloadDao().updateProgress(id, downloaded, speed, eta, status)

    suspend fun updateDownloadStatus(id: String, status: DownloadStatus) =
        database.downloadDao().updateStatus(id, status)

    suspend fun deleteDownload(id: String) = database.downloadDao().deleteDownloadById(id)

    suspend fun clearDownloadsByStatus(status: DownloadStatus) = database.downloadDao().clearDownloadsByStatus(status)

    // Tabs
    val savedTabs: Flow<List<TabEntity>> = database.tabDao().getAllSavedTabs()

    suspend fun saveTabs(tabs: List<TabEntity>) {
        database.tabDao().clearSavedTabs()
        database.tabDao().insertTabs(tabs.filter { !it.isPrivate })
    }

    suspend fun clearSavedTabs() = database.tabDao().clearSavedTabs()

    // Site preferences
    fun getSitePreference(domain: String): Flow<SitePreferenceEntity?> =
        database.sitePreferenceDao().getSitePreference(domain)

    suspend fun getSitePreferenceSync(domain: String): SitePreferenceEntity? =
        database.sitePreferenceDao().getSitePreferenceSync(domain)

    suspend fun setSitePreference(preference: SitePreferenceEntity) =
        database.sitePreferenceDao().insertOrUpdate(preference)

    // Shortcuts
    val shortcuts: Flow<List<ShortcutEntity>> = database.shortcutDao().getAllShortcuts()

    suspend fun addShortcut(title: String, url: String, iconKey: String = "web") {
        database.shortcutDao().insertShortcut(
            ShortcutEntity(
                title = title,
                url = url,
                iconKey = iconKey,
                isDefault = false
            )
        )
    }

    suspend fun deleteShortcut(id: Long) = database.shortcutDao().deleteShortcutById(id)
}
