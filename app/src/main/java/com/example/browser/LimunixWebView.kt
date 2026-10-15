package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.adblock.AdBlockManager
import com.example.adblock.BlockType
import com.example.repository.BoostMode
import com.example.viewmodel.BrowserViewModel
import java.io.ByteArrayInputStream

class LimunixJavaScriptBridge(
    private val onReaderExtracted: (title: String, content: String, wordCount: Int) -> Unit,
    private val onMediaDetected: (url: String, title: String) -> Unit
) {
    @JavascriptInterface
    fun onReaderContent(title: String, content: String, wordCount: Int) {
        onReaderExtracted(title, content, wordCount)
    }

    @JavascriptInterface
    fun onMediaDetected(url: String, title: String) {
        onMediaDetected(url, title)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LimunixWebView(
    tab: LimunixTab,
    viewModel: BrowserViewModel,
    onShowFileChooser: (ValueCallback<Array<Uri>>?) -> Unit,
    onErrorOccurred: (errorCode: Int, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val webView = remember(tab.id) {
        tab.webView ?: WebView(context).apply {
            tab.webView = this
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Settings
            this.settings.apply {
                javaScriptEnabled = settings.javascriptEnabled
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = !settings.dataSaver
                mediaPlaybackRequiresUserGesture = settings.dataSaver
                useWideViewPort = tab.isDesktopMode
                loadWithOverviewMode = tab.isDesktopMode
                displayZoomControls = false
                builtInZoomControls = true
                setSupportZoom(true)
                allowFileAccess = true
                allowContentAccess = true

                userAgentString = if (tab.isDesktopMode) {
                    UrlHelper.DESKTOP_USER_AGENT
                } else {
                    UrlHelper.MOBILE_USER_AGENT
                }

                cacheMode = when (settings.boostMode) {
                    BoostMode.MAX_BOOST -> WebSettings.LOAD_CACHE_ELSE_NETWORK
                    BoostMode.SMART_BOOST -> WebSettings.LOAD_DEFAULT
                    BoostMode.OFF -> WebSettings.LOAD_DEFAULT
                }
            }

            // Private tab cookie isolation
            if (tab.isPrivate) {
                CookieManager.getInstance().setAcceptCookie(false)
            } else {
                CookieManager.getInstance().setAcceptCookie(settings.cookiesEnabled)
            }

            // JavaScript Bridge
            val bridge = LimunixJavaScriptBridge(
                onReaderExtracted = { title, content, count ->
                    viewModel.onReaderContentExtracted(tab.id, title, content, count)
                },
                onMediaDetected = { url, title ->
                    viewModel.onMediaDetected(tab.id, url, title)
                }
            )
            addJavascriptInterface(bridge, "LimunixBridge")

            // FindListener
            setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                viewModel.updateFindMatches(activeMatchOrdinal + 1, numberOfMatches)
            }
        }
    }

    // Configure clients
    LaunchedEffect(webView, settings, tab.isPrivate) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val scheme = request.url.scheme?.lowercase() ?: ""

                // Handle external intents (tel, mailto, sms, market, etc.)
                if (scheme !in listOf("http", "https", "about", "data", "file")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, request.url).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        return true // Consume to prevent crash
                    }
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null

                // Detect direct media requests
                val lowerUrl = url.lowercase()
                if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".m3u8") || lowerUrl.endsWith(".webm")) {
                    view?.post {
                        viewModel.onMediaDetected(tab.id, url, tab.title)
                    }
                }

                // Ad & Tracker blocking check
                val decision = AdBlockManager.checkUrl(
                    url = url,
                    adBlockEnabled = settings.adBlockEnabled,
                    trackerBlockEnabled = settings.trackerBlockEnabled,
                    popupBlockEnabled = settings.popupBlockEnabled,
                    isStrict = settings.strictBlocking || settings.boostMode == BoostMode.MAX_BOOST,
                    whitelist = settings.whitelistDomains,
                    blacklist = settings.blacklistDomains
                )

                if (decision.blocked) {
                    if (decision.type == BlockType.AD) {
                        viewModel.onAdBlocked()
                    } else if (decision.type == BlockType.TRACKER) {
                        viewModel.onTrackerBlocked()
                    }
                    // Return empty response to block request
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream(ByteArray(0))
                    )
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let {
                    viewModel.updateTabLoading(tab.id, true, 10)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    viewModel.updateTabLoading(tab.id, false, 100)
                    viewModel.updateTabUrlAndTitle(tab.id, it, view?.title ?: "")
                    viewModel.updateTabNavigationState(tab.id, view?.canGoBack() == true, view?.canGoForward() == true)

                    // Inject Reader Mode extraction & HTML5 media detection
                    view?.evaluateJavascript(ReaderModeExtractor.JS_EXTRACT_ARTICLE, null)
                    view?.evaluateJavascript(ReaderModeExtractor.JS_DETECT_VIDEO, null)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode ?: -1 else -1
                    val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description?.toString() ?: "Network error" else "Network error"
                    onErrorOccurred(code, desc)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                viewModel.updateTabLoading(tab.id, newProgress < 100, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank() && view?.url != null) {
                    viewModel.updateTabUrlAndTitle(tab.id, view.url ?: "", title)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                onShowFileChooser(filePathCallback)
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // Safely grant media permissions if requested
                request?.grant(request.resources)
            }
        }
    }

    // Load initial URL if not already loaded
    LaunchedEffect(tab.url) {
        if (!tab.isHome && webView.url != tab.url) {
            webView.loadUrl(tab.url)
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
