package com.example.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "limunix_settings")

enum class SearchEngine(val displayName: String, val searchUrl: String, val suggestUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q=", "https://suggestqueries.google.com/complete/search?client=chrome&q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", ""),
    BING("Bing", "https://www.bing.com/search?q=", ""),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=", ""),
    CUSTOM("Custom", "", "")
}

enum class BoostMode {
    OFF,
    SMART_BOOST,
    MAX_BOOST
}

enum class ToolbarPosition {
    BOTTOM,
    TOP
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class LimunixSettings(
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val customSearchUrl: String = "https://duckduckgo.com/?q=",
    val boostMode: BoostMode = BoostMode.SMART_BOOST,
    val dataSaver: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val trackerBlockEnabled: Boolean = true,
    val popupBlockEnabled: Boolean = true,
    val strictBlocking: Boolean = false,
    val toolbarPosition: ToolbarPosition = ToolbarPosition.BOTTOM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val eyeComfortFilter: Boolean = false,
    val eyeComfortIntensity: Float = 0.25f,
    val videoGestureBrightness: Boolean = true,
    val videoGestureVolume: Boolean = true,
    val videoSeekDurationSeconds: Int = 10,
    val videoAutoFullscreen: Boolean = true,
    val videoPipEnabled: Boolean = true,
    val videoBackgroundPlayback: Boolean = true,
    val desktopSiteDefault: Boolean = false,
    val restoreSessionOnStartup: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val whitelistDomains: Set<String> = emptySet(),
    val blacklistDomains: Set<String> = emptySet(),
    val adsBlockedTotal: Int = 0,
    val trackersBlockedTotal: Int = 0
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val CUSTOM_SEARCH_URL = stringPreferencesKey("custom_search_url")
        val BOOST_MODE = stringPreferencesKey("boost_mode")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val TRACKER_BLOCK_ENABLED = booleanPreferencesKey("tracker_block_enabled")
        val POPUP_BLOCK_ENABLED = booleanPreferencesKey("popup_block_enabled")
        val STRICT_BLOCKING = booleanPreferencesKey("strict_blocking")
        val TOOLBAR_POSITION = stringPreferencesKey("toolbar_position")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EYE_COMFORT_FILTER = booleanPreferencesKey("eye_comfort_filter")
        val EYE_COMFORT_INTENSITY = floatPreferencesKey("eye_comfort_intensity")
        val VIDEO_GESTURE_BRIGHTNESS = booleanPreferencesKey("video_gesture_brightness")
        val VIDEO_GESTURE_VOLUME = booleanPreferencesKey("video_gesture_volume")
        val VIDEO_SEEK_DURATION = intPreferencesKey("video_seek_duration")
        val VIDEO_AUTO_FULLSCREEN = booleanPreferencesKey("video_auto_fullscreen")
        val VIDEO_PIP_ENABLED = booleanPreferencesKey("video_pip_enabled")
        val VIDEO_BACKGROUND_PLAYBACK = booleanPreferencesKey("video_background_playback")
        val DESKTOP_SITE_DEFAULT = booleanPreferencesKey("desktop_site_default")
        val RESTORE_SESSION_ON_STARTUP = booleanPreferencesKey("restore_session_on_startup")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val COOKIES_ENABLED = booleanPreferencesKey("cookies_enabled")
        val WHITELIST_DOMAINS = stringSetPreferencesKey("whitelist_domains")
        val BLACKLIST_DOMAINS = stringSetPreferencesKey("blacklist_domains")
        val ADS_BLOCKED_TOTAL = intPreferencesKey("ads_blocked_total")
        val TRACKERS_BLOCKED_TOTAL = intPreferencesKey("trackers_blocked_total")
    }

    val settingsFlow: Flow<LimunixSettings> = context.dataStore.data.map { preferences ->
        LimunixSettings(
            searchEngine = try {
                SearchEngine.valueOf(preferences[PreferencesKeys.SEARCH_ENGINE] ?: SearchEngine.DUCKDUCKGO.name)
            } catch (e: Exception) {
                SearchEngine.DUCKDUCKGO
            },
            customSearchUrl = preferences[PreferencesKeys.CUSTOM_SEARCH_URL] ?: "https://duckduckgo.com/?q=",
            boostMode = try {
                BoostMode.valueOf(preferences[PreferencesKeys.BOOST_MODE] ?: BoostMode.SMART_BOOST.name)
            } catch (e: Exception) {
                BoostMode.SMART_BOOST
            },
            dataSaver = preferences[PreferencesKeys.DATA_SAVER] ?: false,
            adBlockEnabled = preferences[PreferencesKeys.AD_BLOCK_ENABLED] ?: true,
            trackerBlockEnabled = preferences[PreferencesKeys.TRACKER_BLOCK_ENABLED] ?: true,
            popupBlockEnabled = preferences[PreferencesKeys.POPUP_BLOCK_ENABLED] ?: true,
            strictBlocking = preferences[PreferencesKeys.STRICT_BLOCKING] ?: false,
            toolbarPosition = try {
                ToolbarPosition.valueOf(preferences[PreferencesKeys.TOOLBAR_POSITION] ?: ToolbarPosition.BOTTOM.name)
            } catch (e: Exception) {
                ToolbarPosition.BOTTOM
            },
            themeMode = try {
                ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            },
            eyeComfortFilter = preferences[PreferencesKeys.EYE_COMFORT_FILTER] ?: false,
            eyeComfortIntensity = preferences[PreferencesKeys.EYE_COMFORT_INTENSITY] ?: 0.25f,
            videoGestureBrightness = preferences[PreferencesKeys.VIDEO_GESTURE_BRIGHTNESS] ?: true,
            videoGestureVolume = preferences[PreferencesKeys.VIDEO_GESTURE_VOLUME] ?: true,
            videoSeekDurationSeconds = preferences[PreferencesKeys.VIDEO_SEEK_DURATION] ?: 10,
            videoAutoFullscreen = preferences[PreferencesKeys.VIDEO_AUTO_FULLSCREEN] ?: true,
            videoPipEnabled = preferences[PreferencesKeys.VIDEO_PIP_ENABLED] ?: true,
            videoBackgroundPlayback = preferences[PreferencesKeys.VIDEO_BACKGROUND_PLAYBACK] ?: true,
            desktopSiteDefault = preferences[PreferencesKeys.DESKTOP_SITE_DEFAULT] ?: false,
            restoreSessionOnStartup = preferences[PreferencesKeys.RESTORE_SESSION_ON_STARTUP] ?: true,
            javascriptEnabled = preferences[PreferencesKeys.JAVASCRIPT_ENABLED] ?: true,
            cookiesEnabled = preferences[PreferencesKeys.COOKIES_ENABLED] ?: true,
            whitelistDomains = preferences[PreferencesKeys.WHITELIST_DOMAINS] ?: emptySet(),
            blacklistDomains = preferences[PreferencesKeys.BLACKLIST_DOMAINS] ?: emptySet(),
            adsBlockedTotal = preferences[PreferencesKeys.ADS_BLOCKED_TOTAL] ?: 0,
            trackersBlockedTotal = preferences[PreferencesKeys.TRACKERS_BLOCKED_TOTAL] ?: 0
        )
    }

    suspend fun updateSearchEngine(engine: SearchEngine) {
        context.dataStore.edit { it[PreferencesKeys.SEARCH_ENGINE] = engine.name }
    }

    suspend fun updateCustomSearchUrl(url: String) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_SEARCH_URL] = url }
    }

    suspend fun updateBoostMode(mode: BoostMode) {
        context.dataStore.edit { it[PreferencesKeys.BOOST_MODE] = mode.name }
    }

    suspend fun updateDataSaver(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DATA_SAVER] = enabled }
    }

    suspend fun updateAdBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AD_BLOCK_ENABLED] = enabled }
    }

    suspend fun updateTrackerBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TRACKER_BLOCK_ENABLED] = enabled }
    }

    suspend fun updatePopupBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.POPUP_BLOCK_ENABLED] = enabled }
    }

    suspend fun updateStrictBlocking(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.STRICT_BLOCKING] = enabled }
    }

    suspend fun updateToolbarPosition(position: ToolbarPosition) {
        context.dataStore.edit { it[PreferencesKeys.TOOLBAR_POSITION] = position.name }
    }

    suspend fun updateThemeMode(theme: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = theme.name }
    }

    suspend fun updateEyeComfort(enabled: Boolean, intensity: Float? = null) {
        context.dataStore.edit {
            it[PreferencesKeys.EYE_COMFORT_FILTER] = enabled
            if (intensity != null) {
                it[PreferencesKeys.EYE_COMFORT_INTENSITY] = intensity
            }
        }
    }

    suspend fun updateEyeComfortIntensity(intensity: Float) {
        context.dataStore.edit { it[PreferencesKeys.EYE_COMFORT_INTENSITY] = intensity }
    }

    suspend fun updateVideoGestureSettings(brightness: Boolean, volume: Boolean, seekSec: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.VIDEO_GESTURE_BRIGHTNESS] = brightness
            it[PreferencesKeys.VIDEO_GESTURE_VOLUME] = volume
            it[PreferencesKeys.VIDEO_SEEK_DURATION] = seekSec
        }
    }

    suspend fun updateDesktopSiteDefault(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DESKTOP_SITE_DEFAULT] = enabled }
    }

    suspend fun updateRestoreSession(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.RESTORE_SESSION_ON_STARTUP] = enabled }
    }

    suspend fun updateJavascriptEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun updateCookiesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.COOKIES_ENABLED] = enabled }
    }

    suspend fun addWhitelistDomain(domain: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.WHITELIST_DOMAINS] ?: emptySet()
            prefs[PreferencesKeys.WHITELIST_DOMAINS] = current + domain.lowercase()
        }
    }

    suspend fun removeWhitelistDomain(domain: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.WHITELIST_DOMAINS] ?: emptySet()
            prefs[PreferencesKeys.WHITELIST_DOMAINS] = current - domain.lowercase()
        }
    }

    suspend fun addBlacklistDomain(domain: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.BLACKLIST_DOMAINS] ?: emptySet()
            prefs[PreferencesKeys.BLACKLIST_DOMAINS] = current + domain.lowercase()
        }
    }

    suspend fun removeBlacklistDomain(domain: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.BLACKLIST_DOMAINS] ?: emptySet()
            prefs[PreferencesKeys.BLACKLIST_DOMAINS] = current - domain.lowercase()
        }
    }

    suspend fun incrementBlockedStats(adsCount: Int, trackersCount: Int) {
        context.dataStore.edit { prefs ->
            val currentAds = prefs[PreferencesKeys.ADS_BLOCKED_TOTAL] ?: 0
            val currentTrackers = prefs[PreferencesKeys.TRACKERS_BLOCKED_TOTAL] ?: 0
            prefs[PreferencesKeys.ADS_BLOCKED_TOTAL] = currentAds + adsCount
            prefs[PreferencesKeys.TRACKERS_BLOCKED_TOTAL] = currentTrackers + trackersCount
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { it.clear() }
    }
}
