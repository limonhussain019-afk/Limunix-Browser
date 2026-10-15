# Limunix Browser — Native Android Web Browser

Limunix Browser is a modern, high-performance, privacy-focused native Android web browser application built using Kotlin, Jetpack Compose, Material 3, AndroidX Media3 / ExoPlayer, Room database, and DataStore preferences.

---

## 🌟 Key Features

### 1. Main Browser & Navigation
- **Smart Address Bar**: Intelligently distinguishes between web URLs and search queries. Supports leading search engines (DuckDuckGo, Google, Bing, Yahoo, or Custom).
- **Security Indicator**: HTTPS padlock indicator and certificate security inspector.
- **Progress & Controls**: Animated loading bar, refresh, stop, and tab counter button.
- **Top / Bottom Toolbar Options**: User can configure toolbar placement in Settings (bottom toolbar optimized for comfortable one-handed use).

### 2. Multi-Tab System
- **Tab Manager Grid**: Visual card overview of all active tabs.
- **Private Browsing Mode**: Dedicated incognito tabs that bypass persistent browsing history, cookies, and search logs.
- **Tab Controls**: New Tab, Close Tab, Close Other Tabs, Duplicate Tab, Close All Tabs.
- **Session Restoration**: Option to automatically restore open non-private tabs upon restarting the app.
- **Inactive Tab Optimization**: Detaches webview resources on inactive tabs to preserve battery and RAM.

### 3. Native Video Player (Media3 / ExoPlayer)
- **HTML5 Video Interception**: Automatically detects web video sources and prompts the user with *"Open in Limunix Player"*.
- **Full Player Controls**: Play, Pause, Scrub timeline, 10s Rewind/Forward, aspect ratio selection (Fit, Crop, Zoom, 16:9, 4:3), controls lock, and playback speed (0.5x to 2.0x).
- **Gesture Controls**:
  - **Left side vertical swipe**: Adjust screen brightness with on-screen HUD % indicator.
  - **Right side vertical swipe**: Adjust media volume with on-screen HUD % indicator.
  - **Center horizontal swipe**: Seek scrub forward/backward.
  - **Double-tap left/right**: Configurable seek (5s, 10s, 15s, 30s).
  - **Long-press**: Temporary 2.0x playback speed boost while pressed.
- **Picture-in-Picture (PiP)**: Full Android PiP support when navigating to home or tapping the PiP icon.

### 4. Built-In Ad & Tracker Blocker
- **Network-Level Request Interception**: Intercepts web resource requests in `WebViewClient.shouldInterceptRequest`.
- **Known Ad Networks**: Blocks major advertising hosts, tracking pixels, telemetry scripts, and cryptominers.
- **Live Statistics**: Displays real-time ads and trackers blocked counters on the homepage and in site settings.
- **Whitelist & Blacklist**: Allows users to whitelist specific sites where blocking causes issues or blacklist annoying domains.
- **Strict Blocking Mode**: Blocks extra aggressive scripts and popups.

### 5. Download Manager
- **Multi-File Downloads**: Background download manager using OkHttp and Coroutines with HTTP Range support for pause and resume.
- **Live Stats**: Real-time download progress, transfer speed, file size, and ETA calculation.
- **Categorization**: Filters for Videos, Music, Images, and Documents.
- **Actions**: Open file with external viewers, share file, retry failed downloads, and delete.

### 6. Reader Mode
- Extracts article body, headings, and text while stripping sidebars, banner ads, and scripts.
- Customizable font size and reading themes: Light, Sepia, Dark, and OLED Black.

### 7. Customization & Extra Utilities
- **Limunix Boost Mode**:
  - *Off*: Normal browsing.
  - *Smart Boost*: Standard acceleration with intelligent caching.
  - *Max Boost*: Strict caching and aggressive script optimization.
- **Data Saver**: Disables video autoplay and reduces image bandwidth.
- **Night Mode (Eye Comfort Filter)**: Adjustable warm amber overlay with intensity slider.
- **Find in Page**: In-page text search with live match counter and next/previous controls.
- **Webpage Screenshot**: One-tap visible page capture with preview and sharing.
- **Offline Page**: Informative offline recovery screen with quick retry and access to saved downloads.
- **Site Settings**: View SSL security details, toggle ad blocker per-site, and clear site cookies.

---

## 🛠️ How to Open, Build & Customize

### Opening the Project
1. Open Android Studio (Ladybug / Iguana or later recommended).
2. Choose **Open** and select the root directory of this project.
3. Allow Gradle to sync.

### Building
- **Debug APK**:
  Run in terminal:
  ```bash
  gradle :app:assembleDebug
  ```
  The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
- **Release APK**:
  Run:
  ```bash
  gradle :app:assembleRelease
  ```
  The release APK will be generated at `app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if upload credentials are set).

### Running Tests
Run:
```bash
gradle :app:testDebugUnitTest
```

### Changing App Name
- Edit `app/src/main/res/values/strings.xml`:
  ```xml
  <string name="app_name">Limunix Browser</string>
  ```
- Edit `metadata.json`:
  ```json
  "name": "Limunix Browser"
  ```

### Changing Package Name
- Update `namespace` and `applicationId` in `app/build.gradle.kts`.
- Rename package folder structure in `app/src/main/java/com/example`.

### Changing Launcher Icon
- Edit `app/src/main/res/drawable/ic_launcher_foreground.xml` and `ic_launcher_background.xml`.

### Changing Theme Colors
- Edit `app/src/main/java/com/example/ui/theme/Color.kt` and `Theme.kt`.

### Updating Ad-Block Lists
- Add or modify domains in `app/src/main/java/com/example/adblock/AdBlockManager.kt` (`AD_DOMAINS`, `TRACKER_DOMAINS`, `STRICT_DOMAINS`).

---

## 📋 Technical Limitation Report

1. **DRM / Protected Content**: Encrypted media (such as Widevine DRM on protected commercial streaming sites) cannot be routed to external native players without decrypting keys. In those cases, Limunix gracefully lets the website's native web player play the video.
2. **Third-party App Links**: Intent links (`intent://`, `market://`, `tel:`) require appropriate external apps installed on the device. Unhandled schemes are gracefully swallowed to avoid crashing the browser.
3. **Private Tab Isolation**: While private mode isolates cookies and prevents storing history or form data locally, it does not hide network traffic from network administrators or internet service providers.
