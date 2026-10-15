package com.example.browser

import android.webkit.WebView
import com.example.video.DetectedMedia
import java.util.UUID

const val HOME_URL = "limunix://home"

data class LimunixTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = HOME_URL,
    val title: String = "Limunix Home",
    val isPrivate: Boolean = false,
    val isDesktopMode: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isSecure: Boolean = false,
    val detectedMedia: DetectedMedia? = null,
    val lastAccessed: Long = System.currentTimeMillis(),
    val readerArticle: ReaderArticle? = null,
    val isReaderAvailable: Boolean = false,
    var webView: WebView? = null
) {
    val isHome: Boolean
        get() = url == HOME_URL || url.isBlank() || url == "about:blank"
}
