package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.adblock.AdBlockManager
import com.example.browser.HOME_URL
import com.example.browser.LimunixTab
import com.example.browser.LimunixWebView
import com.example.browser.UrlHelper
import com.example.repository.BoostMode
import com.example.repository.ToolbarPosition
import com.example.ui.components.BookmarksScreen
import com.example.ui.components.BrowserBottomBar
import com.example.ui.components.BrowserMenuSheet
import com.example.ui.components.DownloadsScreen
import com.example.ui.components.FindInPageBar
import com.example.ui.components.HistoryScreen
import com.example.ui.components.HomePageView
import com.example.ui.components.NightModeOverlay
import com.example.ui.components.OfflineView
import com.example.ui.components.ReaderModeScreen
import com.example.ui.components.SettingsScreen
import com.example.ui.components.SiteSettingsDialog
import com.example.ui.components.SmartAddressBar
import com.example.ui.components.TabManagerSheet
import com.example.ui.theme.LimunixAzure
import com.example.ui.theme.LimunixCyan
import com.example.video.LimunixNativeVideoPlayer
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun MainBrowserScreen(
    viewModel: BrowserViewModel,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val currentView = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val addressInput by viewModel.addressBarInput.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val currentTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: LimunixTab()

    // Screen and dialog visibility states
    val isTabManagerOpen by viewModel.isTabManagerOpen.collectAsStateWithLifecycle()
    val isMenuSheetOpen by viewModel.isMenuSheetOpen.collectAsStateWithLifecycle()
    val isBookmarksOpen by viewModel.isBookmarksScreenOpen.collectAsStateWithLifecycle()
    val isHistoryOpen by viewModel.isHistoryScreenOpen.collectAsStateWithLifecycle()
    val isDownloadsOpen by viewModel.isDownloadsScreenOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsScreenOpen.collectAsStateWithLifecycle()
    val isSiteSettingsOpen by viewModel.isSiteSettingsDialogOpen.collectAsStateWithLifecycle()
    val isReaderModeActive by viewModel.isReaderModeActive.collectAsStateWithLifecycle()
    val activeReaderArticle by viewModel.activeReaderArticle.collectAsStateWithLifecycle()
    val isFindInPageActive by viewModel.isFindInPageActive.collectAsStateWithLifecycle()
    val isNightModeActive by viewModel.isNightModeActive.collectAsStateWithLifecycle()
    val nightModeIntensity by viewModel.nightModeIntensity.collectAsStateWithLifecycle()
    val activeVideoUrl by viewModel.activeVideoUrl.collectAsStateWithLifecycle()
    val activeVideoTitle by viewModel.activeVideoTitle.collectAsStateWithLifecycle()
    val capturedScreenshot by viewModel.capturedScreenshot.collectAsStateWithLifecycle()

    var isOfflineError by remember { mutableStateOf(false) }

    // File chooser launcher for web upload inputs
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            val uris = if (uri != null) arrayOf(uri) else null
            fileUploadCallback?.onReceiveValue(uris)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    // Android back button handling
    BackHandler {
        when {
            activeVideoUrl != null -> viewModel.closeLimunixPlayer()
            isReaderModeActive -> viewModel.closeReaderMode()
            isFindInPageActive -> viewModel.closeFindInPage()
            isBookmarksOpen -> viewModel.isBookmarksScreenOpen.value = false
            isHistoryOpen -> viewModel.isHistoryScreenOpen.value = false
            isDownloadsOpen -> viewModel.isDownloadsScreenOpen.value = false
            isSettingsOpen -> viewModel.isSettingsScreenOpen.value = false
            isTabManagerOpen -> viewModel.isTabManagerOpen.value = false
            isMenuSheetOpen -> viewModel.isMenuSheetOpen.value = false
            capturedScreenshot != null -> viewModel.capturedScreenshot.value = null
            currentTab.webView?.canGoBack() == true -> currentTab.webView?.goBack()
            !currentTab.isHome -> viewModel.navigateActiveTab(HOME_URL)
            tabs.size > 1 -> viewModel.closeTab(currentTab.id)
            else -> (context as? Activity)?.finish()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                SmartAddressBar(
                    tab = currentTab,
                    input = addressInput,
                    onInputChange = { viewModel.onAddressInputChanged(it) },
                    onSubmit = {
                        isOfflineError = false
                        viewModel.navigateActiveTab(it)
                    },
                    onReload = {
                        isOfflineError = false
                        viewModel.reloadActiveTab()
                    },
                    onStop = { viewModel.stopActiveTab() },
                    onOpenTabs = { viewModel.isTabManagerOpen.value = true },
                    onOpenMenu = { viewModel.isMenuSheetOpen.value = true },
                    onOpenSiteSettings = { viewModel.isSiteSettingsDialogOpen.value = true },
                    tabCount = tabs.size
                )

                if (isFindInPageActive) {
                    val findQuery by viewModel.findQuery.collectAsStateWithLifecycle()
                    val matchCount by viewModel.findMatchCount.collectAsStateWithLifecycle()
                    val currentIndex by viewModel.findCurrentIndex.collectAsStateWithLifecycle()

                    FindInPageBar(
                        query = findQuery,
                        currentIndex = currentIndex,
                        matchCount = matchCount,
                        onQueryChange = { viewModel.onFindQueryChanged(it) },
                        onNext = { viewModel.findNextMatch() },
                        onPrevious = { viewModel.findPreviousMatch() },
                        onClose = { viewModel.closeFindInPage() }
                    )
                }
            }
        },
        bottomBar = {
            if (settings.toolbarPosition == ToolbarPosition.BOTTOM) {
                BrowserBottomBar(
                    canGoBack = currentTab.canGoBack,
                    canGoForward = currentTab.canGoForward,
                    onBack = { isOfflineError = false; viewModel.goBackActiveTab() },
                    onForward = { isOfflineError = false; viewModel.goForwardActiveTab() },
                    onHome = {
                        isOfflineError = false
                        viewModel.navigateActiveTab(HOME_URL)
                    },
                    onTabs = { viewModel.isTabManagerOpen.value = true },
                    onMenu = { viewModel.isMenuSheetOpen.value = true },
                    tabCount = tabs.size
                )
            }
        },
        modifier = Modifier.fillMaxSize().testTag("main_browser_scaffold")
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main Webview or Home Page
            if (isOfflineError) {
                OfflineView(
                    onRetry = {
                        isOfflineError = false
                        viewModel.reloadActiveTab()
                    },
                    onHome = {
                        isOfflineError = false
                        viewModel.navigateActiveTab(HOME_URL)
                    },
                    onOpenDownloads = { viewModel.isDownloadsScreenOpen.value = true }
                )
            } else if (currentTab.isHome) {
                val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
                val frequentlyVisited by viewModel.frequentlyVisited.collectAsStateWithLifecycle()

                HomePageView(
                    shortcuts = shortcuts,
                    frequentlyVisited = frequentlyVisited,
                    boostMode = settings.boostMode,
                    adsBlockedTotal = settings.adsBlockedTotal,
                    trackersBlockedTotal = settings.trackersBlockedTotal,
                    isPrivateMode = currentTab.isPrivate,
                    onNavigate = { viewModel.navigateActiveTab(it) },
                    onOpenSearch = { viewModel.onAddressFocusChanged(true) },
                    onOpenPrivateTab = { viewModel.createNewTab(isPrivate = true) },
                    onOpenDownloads = { viewModel.isDownloadsScreenOpen.value = true },
                    onOpenBookmarks = { viewModel.isBookmarksScreenOpen.value = true },
                    onOpenHistory = { viewModel.isHistoryScreenOpen.value = true },
                    onAddShortcut = { title, url ->
                        scope.launch { viewModel.browserRepository.addShortcut(title, url) }
                    },
                    onDeleteShortcut = { id ->
                        scope.launch { viewModel.browserRepository.deleteShortcut(id) }
                    }
                )
            } else {
                LimunixWebView(
                    tab = currentTab,
                    viewModel = viewModel,
                    onShowFileChooser = { callback ->
                        fileUploadCallback = callback
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        fileChooserLauncher.launch(Intent.createChooser(intent, "Select File"))
                    },
                    onErrorOccurred = { _, _ ->
                        isOfflineError = true
                    }
                )
            }

            // Night Mode Overlay
            if (isNightModeActive) {
                NightModeOverlay(intensity = nightModeIntensity)
            }

            // Detected Video Banner: Floating action to open in Limunix Player
            AnimatedVisibility(
                visible = currentTab.detectedMedia != null && !currentTab.isHome && activeVideoUrl == null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                currentTab.detectedMedia?.let { media ->
                    Surface(
                        onClick = {
                            viewModel.openLimunixPlayer(media.url, media.title)
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = LimunixCyan,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .testTag("open_limunix_player_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open in Limunix Player",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Native Video Player Overlay
    if (activeVideoUrl != null) {
        LimunixNativeVideoPlayer(
            videoUrl = activeVideoUrl ?: "",
            videoTitle = activeVideoTitle,
            seekDurationSeconds = settings.videoSeekDurationSeconds,
            onClose = { viewModel.closeLimunixPlayer() },
            onEnterPip = onEnterPip
        )
    }

    // Reader Mode Screen
    if (isReaderModeActive && activeReaderArticle != null) {
        val readerTheme by viewModel.readerTheme.collectAsStateWithLifecycle()
        val readerFontSize by viewModel.readerFontSize.collectAsStateWithLifecycle()

        ReaderModeScreen(
            article = activeReaderArticle!!,
            theme = readerTheme,
            fontSize = readerFontSize,
            onClose = { viewModel.closeReaderMode() },
            onThemeChange = { viewModel.setReaderTheme(it) },
            onFontSizeChange = { viewModel.adjustReaderFontSize(it) }
        )
    }

    // Bookmarks Screen
    if (isBookmarksOpen) {
        val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
        BookmarksScreen(
            bookmarks = bookmarks,
            onClose = { viewModel.isBookmarksScreenOpen.value = false },
            onOpenBookmark = {
                viewModel.navigateActiveTab(it)
                viewModel.isBookmarksScreenOpen.value = false
            },
            onOpenInNewTab = { url, isPrivate ->
                viewModel.createNewTab(url, isPrivate = isPrivate)
                viewModel.isBookmarksScreenOpen.value = false
            },
            onDeleteBookmark = { id ->
                scope.launch { viewModel.browserRepository.deleteBookmark(id) }
            },
            onAddBookmark = { title, url, folder ->
                scope.launch { viewModel.browserRepository.addBookmark(url, title, folder) }
            }
        )
    }

    // History Screen
    if (isHistoryOpen) {
        val historyList by viewModel.allHistory.collectAsStateWithLifecycle()
        HistoryScreen(
            historyList = historyList,
            onClose = { viewModel.isHistoryScreenOpen.value = false },
            onNavigate = {
                viewModel.navigateActiveTab(it)
                viewModel.isHistoryScreenOpen.value = false
            },
            onDeleteItem = { id ->
                scope.launch { viewModel.browserRepository.deleteHistoryItem(id) }
            },
            onDeleteSince = { timestamp ->
                scope.launch { viewModel.browserRepository.deleteHistorySince(timestamp) }
            },
            onClearAll = {
                scope.launch { viewModel.browserRepository.clearAllHistory() }
            }
        )
    }

    // Downloads Screen
    if (isDownloadsOpen) {
        val downloadsList by viewModel.allDownloads.collectAsStateWithLifecycle()
        DownloadsScreen(
            downloads = downloadsList,
            onClose = { viewModel.isDownloadsScreenOpen.value = false },
            onPause = { viewModel.downloadManager.pauseDownload(it) },
            onResume = { viewModel.downloadManager.resumeDownload(it) },
            onCancel = { viewModel.downloadManager.cancelDownload(it) },
            onRetry = { viewModel.downloadManager.retryDownload(it) },
            onDelete = { viewModel.downloadManager.deleteDownload(it) },
            onOpenFile = { path, mime -> viewModel.downloadManager.openDownloadedFile(path, mime) },
            onShareFile = { path, mime -> viewModel.downloadManager.shareDownloadedFile(path, mime) }
        )
    }

    // Settings Screen
    if (isSettingsOpen) {
        SettingsScreen(
            settings = settings,
            onClose = { viewModel.isSettingsScreenOpen.value = false },
            onSearchEngineChange = { scope.launch { viewModel.settingsRepository.updateSearchEngine(it) } },
            onBoostModeChange = { viewModel.setBoostMode(it) },
            onDataSaverChange = { viewModel.toggleDataSaver() },
            onAdBlockChange = { scope.launch { viewModel.settingsRepository.updateAdBlockEnabled(it) } },
            onTrackerBlockChange = { scope.launch { viewModel.settingsRepository.updateTrackerBlockEnabled(it) } },
            onPopupBlockChange = { scope.launch { viewModel.settingsRepository.updatePopupBlockEnabled(it) } },
            onStrictModeChange = { scope.launch { viewModel.settingsRepository.updateStrictBlocking(it) } },
            onToolbarPositionChange = { scope.launch { viewModel.settingsRepository.updateToolbarPosition(it) } },
            onThemeModeChange = { scope.launch { viewModel.settingsRepository.updateThemeMode(it) } },
            onVideoGesturesChange = { b, v, s -> scope.launch { viewModel.settingsRepository.updateVideoGestureSettings(b, v, s) } },
            onDesktopDefaultChange = { scope.launch { viewModel.settingsRepository.updateDesktopSiteDefault(it) } },
            onRestoreSessionChange = { scope.launch { viewModel.settingsRepository.updateRestoreSession(it) } },
            onJavascriptChange = { scope.launch { viewModel.settingsRepository.updateJavascriptEnabled(it) } },
            onCookiesChange = { scope.launch { viewModel.settingsRepository.updateCookiesEnabled(it) } },
            onClearBrowsingData = { viewModel.clearBrowsingData() },
            onResetSettings = { scope.launch { viewModel.settingsRepository.resetAllSettings() } }
        )
    }

    // Site Settings Dialog
    if (isSiteSettingsOpen) {
        val domain = UrlHelper.extractDisplayDomain(currentTab.url)
        val isWhitelisted = settings.whitelistDomains.contains(domain)

        SiteSettingsDialog(
            tab = currentTab,
            isAdBlockGloballyEnabled = settings.adBlockEnabled,
            isWhitelisted = isWhitelisted,
            onDismiss = { viewModel.isSiteSettingsDialogOpen.value = false },
            onToggleWhitelist = { shouldWhitelist ->
                scope.launch {
                    if (shouldWhitelist) {
                        viewModel.settingsRepository.addWhitelistDomain(domain)
                    } else {
                        viewModel.settingsRepository.removeWhitelistDomain(domain)
                    }
                }
            },
            onClearSiteData = {
                viewModel.clearBrowsingData(cookies = true, cache = true, history = false)
                Toast.makeText(context, "Site cookies & cache cleared", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Browser Menu Bottom Sheet
    if (isMenuSheetOpen) {
        BrowserMenuSheet(
            tab = currentTab,
            isDesktopMode = currentTab.isDesktopMode,
            isReaderAvailable = currentTab.isReaderAvailable,
            isReaderActive = isReaderModeActive,
            isNightModeActive = isNightModeActive,
            nightModeIntensity = nightModeIntensity,
            boostMode = settings.boostMode,
            dataSaverActive = settings.dataSaver,
            onDismiss = { viewModel.isMenuSheetOpen.value = false },
            onNewTab = { viewModel.createNewTab() },
            onNewPrivateTab = { viewModel.createNewTab(isPrivate = true) },
            onToggleDesktopMode = { viewModel.toggleDesktopModeForActiveTab() },
            onToggleReaderMode = { viewModel.toggleReaderMode() },
            onToggleNightMode = { viewModel.toggleNightMode() },
            onNightModeIntensityChange = { viewModel.setNightModeIntensity(it) },
            onToggleBoostMode = {
                val next = when (settings.boostMode) {
                    BoostMode.OFF -> BoostMode.SMART_BOOST
                    BoostMode.SMART_BOOST -> BoostMode.MAX_BOOST
                    BoostMode.MAX_BOOST -> BoostMode.OFF
                }
                viewModel.setBoostMode(next)
            },
            onToggleDataSaver = { viewModel.toggleDataSaver() },
            onOpenBookmarks = { viewModel.isBookmarksScreenOpen.value = true },
            onOpenHistory = { viewModel.isHistoryScreenOpen.value = true },
            onOpenDownloads = { viewModel.isDownloadsScreenOpen.value = true },
            onOpenFindInPage = { viewModel.openFindInPage() },
            onTakeScreenshot = {
                // Capture visible Webview bitmap
                try {
                    val bitmap = Bitmap.createBitmap(
                        currentView.width.coerceAtLeast(100),
                        currentView.height.coerceAtLeast(100),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    currentView.draw(canvas)
                    viewModel.capturedScreenshot.value = bitmap
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to capture screenshot", Toast.LENGTH_SHORT).show()
                }
            },
            onTranslatePage = {
                val urlToTranslate = currentTab.url
                if (!currentTab.isHome) {
                    val translateUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=${Uri.encode(urlToTranslate)}"
                    viewModel.navigateActiveTab(translateUrl)
                } else {
                    Toast.makeText(context, "Open a website to translate", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenSiteSettings = { viewModel.isSiteSettingsDialogOpen.value = true },
            onOpenSettings = { viewModel.isSettingsScreenOpen.value = true }
        )
    }

    // Tab Manager Sheet
    if (isTabManagerOpen) {
        TabManagerSheet(
            tabs = tabs,
            activeTabId = activeTabId,
            onDismiss = { viewModel.isTabManagerOpen.value = false },
            onSelectTab = { viewModel.switchTab(it) },
            onCloseTab = { viewModel.closeTab(it) },
            onNewTab = { isPrivate -> viewModel.createNewTab(isPrivate = isPrivate) },
            onCloseAllTabs = { viewModel.closeAllTabs() },
            onCloseOtherTabs = { viewModel.closeOtherTabs(it) },
            onDuplicateTab = { viewModel.duplicateTab(it) },
            onRestoreClosedTab = { viewModel.restoreClosedTab() }
        )
    }

    // Screenshot Preview Dialog
    capturedScreenshot?.let { bmp ->
        AlertDialog(
            onDismissRequest = { viewModel.capturedScreenshot.value = null },
            title = { Text("Webpage Screenshot") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Screenshot preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val file = File(context.cacheDir, "limunix_screenshot_${System.currentTimeMillis()}.png")
                            FileOutputStream(file).use { out ->
                                bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Screenshot"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to share screenshot", Toast.LENGTH_SHORT).show()
                        }
                        viewModel.capturedScreenshot.value = null
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.capturedScreenshot.value = null }) {
                    Text("Close")
                }
            }
        )
    }
}
