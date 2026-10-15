package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adblock.AdBlockManager
import com.example.browser.HOME_URL
import com.example.browser.LimunixTab
import com.example.browser.ReaderArticle
import com.example.browser.ReaderTheme
import com.example.browser.UrlHelper
import com.example.database.LimunixDatabase
import com.example.database.entities.BookmarkEntity
import com.example.database.entities.DownloadEntity
import com.example.database.entities.HistoryEntity
import com.example.database.entities.ShortcutEntity
import com.example.database.entities.SitePreferenceEntity
import com.example.database.entities.TabEntity
import com.example.downloads.LimunixDownloadManager
import com.example.repository.BoostMode
import com.example.repository.BrowserRepository
import com.example.repository.LimunixSettings
import com.example.repository.SearchEngine
import com.example.repository.SettingsRepository
import com.example.repository.ToolbarPosition
import com.example.video.DetectedMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Stack

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LimunixDatabase.getDatabase(application)
    val browserRepository = BrowserRepository(database)
    val settingsRepository = SettingsRepository(application)
    val downloadManager = LimunixDownloadManager(application, browserRepository)

    // App settings flow
    val settings: StateFlow<LimunixSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LimunixSettings())

    // Tabs state
    private val _tabs = MutableStateFlow<List<LimunixTab>>(listOf(LimunixTab()))
    val tabs: StateFlow<List<LimunixTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val closedTabsStack = Stack<LimunixTab>()

    // Address bar input text state (distinct from current tab URL when user is editing)
    private val _addressBarInput = MutableStateFlow("")
    val addressBarInput: StateFlow<String> = _addressBarInput.asStateFlow()

    private val _isAddressBarFocused = MutableStateFlow(false)
    val isAddressBarFocused: StateFlow<Boolean> = _isAddressBarFocused.asStateFlow()

    // Real-time blocked stats
    private val _sessionAdsBlocked = MutableStateFlow(0)
    val sessionAdsBlocked: StateFlow<Int> = _sessionAdsBlocked.asStateFlow()

    private val _sessionTrackersBlocked = MutableStateFlow(0)
    val sessionTrackersBlocked: StateFlow<Int> = _sessionTrackersBlocked.asStateFlow()

    // Native Video Player state
    private val _activeVideoUrl = MutableStateFlow<String?>(null)
    val activeVideoUrl: StateFlow<String?> = _activeVideoUrl.asStateFlow()

    private val _activeVideoTitle = MutableStateFlow("Web Video")
    val activeVideoTitle: StateFlow<String> = _activeVideoTitle.asStateFlow()

    // Reader Mode state
    private val _isReaderModeActive = MutableStateFlow(false)
    val isReaderModeActive: StateFlow<Boolean> = _isReaderModeActive.asStateFlow()

    private val _activeReaderArticle = MutableStateFlow<ReaderArticle?>(null)
    val activeReaderArticle: StateFlow<ReaderArticle?> = _activeReaderArticle.asStateFlow()

    private val _readerTheme = MutableStateFlow(ReaderTheme.LIGHT)
    val readerTheme: StateFlow<ReaderTheme> = _readerTheme.asStateFlow()

    private val _readerFontSize = MutableStateFlow(18)
    val readerFontSize: StateFlow<Int> = _readerFontSize.asStateFlow()

    // Find in Page state
    private val _isFindInPageActive = MutableStateFlow(false)
    val isFindInPageActive: StateFlow<Boolean> = _isFindInPageActive.asStateFlow()

    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()

    private val _findMatchCount = MutableStateFlow(0)
    val findMatchCount: StateFlow<Int> = _findMatchCount.asStateFlow()

    private val _findCurrentIndex = MutableStateFlow(0)
    val findCurrentIndex: StateFlow<Int> = _findCurrentIndex.asStateFlow()

    // Night Mode (Eye comfort) state
    private val _isNightModeActive = MutableStateFlow(false)
    val isNightModeActive: StateFlow<Boolean> = _isNightModeActive.asStateFlow()

    private val _nightModeIntensity = MutableStateFlow(0.25f)
    val nightModeIntensity: StateFlow<Float> = _nightModeIntensity.asStateFlow()

    // UI overlays and sheets
    val isTabManagerOpen = MutableStateFlow(false)
    val isMenuSheetOpen = MutableStateFlow(false)
    val isBookmarksScreenOpen = MutableStateFlow(false)
    val isHistoryScreenOpen = MutableStateFlow(false)
    val isDownloadsScreenOpen = MutableStateFlow(false)
    val isSettingsScreenOpen = MutableStateFlow(false)
    val isSiteSettingsDialogOpen = MutableStateFlow(false)
    val capturedScreenshot = MutableStateFlow<Bitmap?>(null)

    // Data streams from repository
    val allBookmarks: StateFlow<List<BookmarkEntity>> = browserRepository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<HistoryEntity>> = browserRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frequentlyVisited: StateFlow<List<HistoryEntity>> = browserRepository.frequentlyVisited
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDownloads: StateFlow<List<DownloadEntity>> = browserRepository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcuts: StateFlow<List<ShortcutEntity>> = browserRepository.shortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTab: LimunixTab?
        get() = _tabs.value.find { it.id == _activeTabId.value }

    init {
        // Restore session if enabled
        viewModelScope.launch {
            val currentSettings = settingsRepository.settingsFlow.first()
            _isNightModeActive.value = currentSettings.eyeComfortFilter
            _nightModeIntensity.value = currentSettings.eyeComfortIntensity

            if (currentSettings.restoreSessionOnStartup) {
                val saved = browserRepository.savedTabs.first()
                if (saved.isNotEmpty()) {
                    val restoredList = saved.map { entity ->
                        LimunixTab(
                            id = entity.id,
                            url = entity.url,
                            title = entity.title,
                            isPrivate = false,
                            isDesktopMode = entity.isDesktopMode
                        )
                    }
                    _tabs.value = restoredList
                    _activeTabId.value = restoredList.first().id
                }
            }
        }
    }

    // Tab Management
    fun createNewTab(url: String = HOME_URL, isPrivate: Boolean = false, inBackground: Boolean = false): String {
        val newTab = LimunixTab(
            url = url,
            title = if (url == HOME_URL) "Limunix Home" else url,
            isPrivate = isPrivate,
            isDesktopMode = settings.value.desktopSiteDefault
        )
        _tabs.update { it + newTab }
        if (!inBackground) {
            _activeTabId.value = newTab.id
            _addressBarInput.value = if (url == HOME_URL) "" else url
            isTabManagerOpen.value = false
        }
        persistNonPrivateTabs()
        return newTab.id
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        val tabToClose = currentList.find { it.id == tabId } ?: return

        // Clean up WebView resources
        tabToClose.webView?.destroy()
        tabToClose.webView = null

        if (!tabToClose.isPrivate && !tabToClose.isHome) {
            closedTabsStack.push(tabToClose)
        }

        if (currentList.size <= 1) {
            // Re-initialize with a single fresh home tab
            val freshTab = LimunixTab(isPrivate = tabToClose.isPrivate)
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
            _addressBarInput.value = ""
        } else {
            val index = currentList.indexOfFirst { it.id == tabId }
            val updated = currentList.filter { it.id != tabId }
            _tabs.value = updated

            if (_activeTabId.value == tabId) {
                val newActiveIndex = if (index >= updated.size) updated.size - 1 else index
                val newActive = updated[newActiveIndex]
                _activeTabId.value = newActive.id
                _addressBarInput.value = if (newActive.isHome) "" else newActive.url
            }
        }
        persistNonPrivateTabs()
    }

    fun switchTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        _activeTabId.value = tab.id
        _addressBarInput.value = if (tab.isHome) "" else tab.url
        isTabManagerOpen.value = false
    }

    fun closeAllTabs() {
        _tabs.value.forEach { it.webView?.destroy() }
        val fresh = LimunixTab()
        _tabs.value = listOf(fresh)
        _activeTabId.value = fresh.id
        _addressBarInput.value = ""
        isTabManagerOpen.value = false
        persistNonPrivateTabs()
    }

    fun closeOtherTabs(keepTabId: String) {
        val keep = _tabs.value.find { it.id == keepTabId } ?: return
        _tabs.value.filter { it.id != keepTabId }.forEach { it.webView?.destroy() }
        _tabs.value = listOf(keep)
        _activeTabId.value = keep.id
        persistNonPrivateTabs()
    }

    fun duplicateTab(tabId: String) {
        val original = _tabs.value.find { it.id == tabId } ?: return
        createNewTab(url = original.url, isPrivate = original.isPrivate)
    }

    fun restoreClosedTab() {
        if (closedTabsStack.isNotEmpty()) {
            val restored = closedTabsStack.pop()
            createNewTab(url = restored.url, isPrivate = false)
        }
    }

    private fun persistNonPrivateTabs() {
        viewModelScope.launch {
            val toSave = _tabs.value
                .filter { !it.isPrivate && !it.isHome }
                .mapIndexed { index, tab ->
                    TabEntity(
                        id = tab.id,
                        title = tab.title,
                        url = tab.url,
                        isPrivate = false,
                        isDesktopMode = tab.isDesktopMode,
                        position = index
                    )
                }
            browserRepository.saveTabs(toSave)
        }
    }

    // Navigation
    fun onAddressInputChanged(input: String) {
        _addressBarInput.value = input
    }

    fun onAddressFocusChanged(focused: Boolean) {
        _isAddressBarFocused.value = focused
        if (!focused) {
            val tab = currentTab
            _addressBarInput.value = if (tab?.isHome == true) "" else tab?.url ?: ""
        }
    }

    fun navigateActiveTab(input: String) {
        val resolved = UrlHelper.resolveInput(
            input = input,
            searchEngine = settings.value.searchEngine,
            customSearchUrl = settings.value.customSearchUrl
        )
        val tabId = _activeTabId.value
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(url = resolved, isLoading = true) else it }
        }
        _addressBarInput.value = resolved
        _isAddressBarFocused.value = false
    }

    fun updateTabUrlAndTitle(tabId: String, url: String, title: String) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) {
                    it.copy(
                        url = url,
                        title = if (title.isBlank()) UrlHelper.extractDisplayDomain(url) else title,
                        isSecure = UrlHelper.isHttps(url)
                    )
                } else it
            }
        }
        if (_activeTabId.value == tabId) {
            _addressBarInput.value = url
        }

        // Add to history if not private and not home
        val tab = _tabs.value.find { it.id == tabId }
        if (tab?.isPrivate != true && url != HOME_URL) {
            viewModelScope.launch {
                browserRepository.addHistory(url, title)
            }
        }
        persistNonPrivateTabs()
    }

    fun updateTabLoading(tabId: String, isLoading: Boolean, progress: Int) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(isLoading = isLoading, progress = progress) else it
            }
        }
    }

    fun updateTabNavigationState(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(canGoBack = canGoBack, canGoForward = canGoForward) else it
            }
        }
    }

    fun toggleDesktopModeForActiveTab() {
        val tabId = _activeTabId.value
        val tab = currentTab ?: return
        val newDesktopMode = !tab.isDesktopMode
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(isDesktopMode = newDesktopMode) else it }
        }
        // Reload tab with new user agent
        tab.webView?.let { webView ->
            webView.settings.userAgentString = if (newDesktopMode) {
                UrlHelper.DESKTOP_USER_AGENT
            } else {
                UrlHelper.MOBILE_USER_AGENT
            }
            webView.settings.useWideViewPort = newDesktopMode
            webView.settings.loadWithOverviewMode = newDesktopMode
            webView.reload()
        }
    }

    fun reloadActiveTab() {
        currentTab?.webView?.reload()
    }

    fun stopActiveTab() {
        currentTab?.webView?.stopLoading()
    }

    fun goBackActiveTab(): Boolean {
        val tab = currentTab
        if (tab?.webView?.canGoBack() == true) {
            tab.webView?.goBack()
            return true
        }
        return false
    }

    fun goForwardActiveTab(): Boolean {
        val tab = currentTab
        if (tab?.webView?.canGoForward() == true) {
            tab.webView?.goForward()
            return true
        }
        return false
    }

    // Media & Video Player
    fun onMediaDetected(tabId: String, mediaUrl: String, title: String) {
        val detected = DetectedMedia(url = mediaUrl, title = title)
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(detectedMedia = detected) else it }
        }
    }

    fun openLimunixPlayer(url: String, title: String = "Web Video") {
        _activeVideoUrl.value = url
        _activeVideoTitle.value = title
    }

    fun closeLimunixPlayer() {
        _activeVideoUrl.value = null
    }

    // Reader Mode
    fun onReaderContentExtracted(tabId: String, title: String, contentHtml: String, wordCount: Int) {
        if (contentHtml.isBlank() || wordCount < 30) {
            _tabs.update { list ->
                list.map { if (it.id == tabId) it.copy(isReaderAvailable = false, readerArticle = null) else it }
            }
            return
        }
        val minutes = (wordCount / 200).coerceAtLeast(1)
        val article = ReaderArticle(
            title = title,
            contentHtml = contentHtml,
            wordCount = wordCount,
            estimatedMinutes = minutes
        )
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(isReaderAvailable = true, readerArticle = article) else it }
        }
    }

    fun toggleReaderMode() {
        val tab = currentTab ?: return
        if (tab.readerArticle != null) {
            _activeReaderArticle.value = tab.readerArticle
            _isReaderModeActive.value = !_isReaderModeActive.value
        }
    }

    fun closeReaderMode() {
        _isReaderModeActive.value = false
    }

    fun setReaderTheme(theme: ReaderTheme) {
        _readerTheme.value = theme
    }

    fun adjustReaderFontSize(delta: Int) {
        _readerFontSize.update { (it + delta).coerceIn(12, 32) }
    }

    // Find In Page
    fun openFindInPage() {
        _isFindInPageActive.value = true
        _findQuery.value = ""
        _findMatchCount.value = 0
        _findCurrentIndex.value = 0
    }

    fun closeFindInPage() {
        _isFindInPageActive.value = false
        currentTab?.webView?.clearMatches()
    }

    fun onFindQueryChanged(query: String) {
        _findQuery.value = query
        currentTab?.webView?.findAllAsync(query)
    }

    fun findNextMatch() {
        currentTab?.webView?.findNext(true)
    }

    fun findPreviousMatch() {
        currentTab?.webView?.findNext(false)
    }

    fun updateFindMatches(currentIndex: Int, totalMatches: Int) {
        _findCurrentIndex.value = currentIndex
        _findMatchCount.value = totalMatches
    }

    // Ad Block & Tracker Block Stats
    fun onAdBlocked() {
        _sessionAdsBlocked.update { it + 1 }
        viewModelScope.launch {
            settingsRepository.incrementBlockedStats(1, 0)
        }
    }

    fun onTrackerBlocked() {
        _sessionTrackersBlocked.update { it + 1 }
        viewModelScope.launch {
            settingsRepository.incrementBlockedStats(0, 1)
        }
    }

    // Bookmarks
    fun toggleBookmarkForCurrentPage() {
        val tab = currentTab ?: return
        if (tab.isHome) return
        viewModelScope.launch {
            val isBookmarked = browserRepository.isBookmarked(tab.url).first()
            if (isBookmarked) {
                browserRepository.deleteBookmarkByUrl(tab.url)
            } else {
                browserRepository.addBookmark(url = tab.url, title = tab.title)
            }
        }
    }

    // Night Mode / Eye Comfort Filter
    fun toggleNightMode() {
        val newState = !_isNightModeActive.value
        _isNightModeActive.value = newState
        viewModelScope.launch {
            settingsRepository.updateEyeComfort(newState)
        }
    }

    fun setNightModeIntensity(intensity: Float) {
        _nightModeIntensity.value = intensity
        viewModelScope.launch {
            settingsRepository.updateEyeComfortIntensity(intensity)
        }
    }

    // Boost Mode & Data Saver
    fun setBoostMode(mode: BoostMode) {
        viewModelScope.launch {
            settingsRepository.updateBoostMode(mode)
        }
    }

    fun toggleDataSaver() {
        viewModelScope.launch {
            val current = settings.value.dataSaver
            settingsRepository.updateDataSaver(!current)
        }
    }

    // Clear Browsing Data
    fun clearBrowsingData(cookies: Boolean = true, cache: Boolean = true, history: Boolean = true) {
        viewModelScope.launch {
            if (history) {
                browserRepository.clearAllHistory()
            }
            if (cookies) {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            if (cache) {
                WebStorage.getInstance().deleteAllData()
                currentTab?.webView?.clearCache(true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _tabs.value.forEach {
            it.webView?.destroy()
        }
    }
}
