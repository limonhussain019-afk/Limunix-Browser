package com.example

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.browser.HOME_URL
import com.example.repository.ThemeMode
import com.example.ui.MainBrowserScreen
import com.example.ui.theme.LimunixTheme
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming URL intents (from external apps or launcher)
        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val tabs by viewModel.tabs.collectAsStateWithLifecycle()
            val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
            val currentTab = tabs.find { it.id == activeTabId }

            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            LimunixTheme(
                darkTheme = darkTheme,
                isPrivateMode = currentTab?.isPrivate == true
            ) {
                MainBrowserScreen(
                    viewModel = viewModel,
                    onEnterPip = { enterPipMode() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val data = intent?.data
        if (Intent.ACTION_VIEW == action && data != null) {
            val url = data.toString()
            if (url.isNotBlank()) {
                viewModel.createNewTab(url)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If native video player is active, enter Picture-in-Picture on Home press
        if (viewModel.activeVideoUrl.value != null && viewModel.settings.value.videoPipEnabled) {
            enterPipMode()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Ignore PiP failure
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Adjust UI if needed in PiP mode
    }
}
