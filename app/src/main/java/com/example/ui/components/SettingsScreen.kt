package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repository.BoostMode
import com.example.repository.LimunixSettings
import com.example.repository.SearchEngine
import com.example.repository.ThemeMode
import com.example.repository.ToolbarPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: LimunixSettings,
    onClose: () -> Unit,
    onSearchEngineChange: (SearchEngine) -> Unit,
    onBoostModeChange: (BoostMode) -> Unit,
    onDataSaverChange: (Boolean) -> Unit,
    onAdBlockChange: (Boolean) -> Unit,
    onTrackerBlockChange: (Boolean) -> Unit,
    onPopupBlockChange: (Boolean) -> Unit,
    onStrictModeChange: (Boolean) -> Unit,
    onToolbarPositionChange: (ToolbarPosition) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onVideoGesturesChange: (brightness: Boolean, volume: Boolean, seekSec: Int) -> Unit,
    onDesktopDefaultChange: (Boolean) -> Unit,
    onRestoreSessionChange: (Boolean) -> Unit,
    onJavascriptChange: (Boolean) -> Unit,
    onCookiesChange: (Boolean) -> Unit,
    onClearBrowsingData: () -> Unit,
    onResetSettings: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier.testTag("settings_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: General
            SettingsSectionHeader(title = "General", icon = Icons.Default.Search)
            SettingsCard {
                // Search Engine Picker
                var searchMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { searchMenuExpanded = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Search Engine", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(settings.searchEngine.displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = searchMenuExpanded,
                        onDismissRequest = { searchMenuExpanded = false }
                    ) {
                        SearchEngine.values().forEach { engine ->
                            DropdownMenuItem(
                                text = { Text(engine.displayName) },
                                onClick = {
                                    onSearchEngineChange(engine)
                                    searchMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider()

                // Restore previous session
                SettingsToggleRow(
                    title = "Restore previous session",
                    subtitle = "Reopen previous tabs on startup",
                    checked = settings.restoreSessionOnStartup,
                    onCheckedChange = onRestoreSessionChange
                )
            }

            // Section: Appearance
            SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Brightness6)
            SettingsCard {
                // Theme Mode
                var themeMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { themeMenuExpanded = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(settings.themeMode.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false }
                    ) {
                        ThemeMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name) },
                                onClick = {
                                    onThemeModeChange(mode)
                                    themeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider()

                // Toolbar Position (Bottom / Top)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = if (settings.toolbarPosition == ToolbarPosition.BOTTOM) ToolbarPosition.TOP else ToolbarPosition.BOTTOM
                            onToolbarPositionChange(next)
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Toolbar Position", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(if (settings.toolbarPosition == ToolbarPosition.BOTTOM) "Bottom (One-hand friendly)" else "Top", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(settings.toolbarPosition.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }

            // Section: Boost Mode & Speed
            SettingsSectionHeader(title = "Limunix Boost & Data", icon = Icons.Default.Bolt)
            SettingsCard {
                var boostMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { boostMenuExpanded = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Limunix Boost Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            when (settings.boostMode) {
                                BoostMode.MAX_BOOST -> "Max Boost (Aggressive caching & rendering)"
                                BoostMode.SMART_BOOST -> "Smart Boost (Standard Web acceleration)"
                                BoostMode.OFF -> "Off"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = boostMenuExpanded,
                        onDismissRequest = { boostMenuExpanded = false }
                    ) {
                        BoostMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name) },
                                onClick = {
                                    onBoostModeChange(mode)
                                    boostMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider()

                SettingsToggleRow(
                    title = "Data Saver",
                    subtitle = "Blocks unnecessary resources & reduces image size",
                    checked = settings.dataSaver,
                    onCheckedChange = onDataSaverChange
                )
            }

            // Section: Ad & Tracker Blocker
            SettingsSectionHeader(title = "Shield & Security", icon = Icons.Default.Shield)
            SettingsCard {
                SettingsToggleRow(
                    title = "Ad Blocker",
                    subtitle = "Blocks advertising networks & scripts",
                    checked = settings.adBlockEnabled,
                    onCheckedChange = onAdBlockChange
                )
                Divider()
                SettingsToggleRow(
                    title = "Block Trackers",
                    subtitle = "Blocks telemetry, analytics, and fingerprinting",
                    checked = settings.trackerBlockEnabled,
                    onCheckedChange = onTrackerBlockChange
                )
                Divider()
                SettingsToggleRow(
                    title = "Block Popups & Redirects",
                    subtitle = "Prevents deceptive windows & automatic app launches",
                    checked = settings.popupBlockEnabled,
                    onCheckedChange = onPopupBlockChange
                )
                Divider()
                SettingsToggleRow(
                    title = "Strict Blocking",
                    subtitle = "Blocks cryptominers & invasive push requests",
                    checked = settings.strictBlocking,
                    onCheckedChange = onStrictModeChange
                )
            }

            // Section: Native Video Player
            SettingsSectionHeader(title = "Native Video Player", icon = Icons.Default.Movie)
            SettingsCard {
                SettingsToggleRow(
                    title = "Brightness Gesture",
                    subtitle = "Vertical swipe on left side adjusts screen brightness",
                    checked = settings.videoGestureBrightness,
                    onCheckedChange = { onVideoGesturesChange(it, settings.videoGestureVolume, settings.videoSeekDurationSeconds) }
                )
                Divider()
                SettingsToggleRow(
                    title = "Volume Gesture",
                    subtitle = "Vertical swipe on right side adjusts media volume",
                    checked = settings.videoGestureVolume,
                    onCheckedChange = { onVideoGesturesChange(settings.videoGestureBrightness, it, settings.videoSeekDurationSeconds) }
                )
                Divider()

                var seekMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { seekMenuExpanded = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Double-Tap Seek Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${settings.videoSeekDurationSeconds} seconds", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = seekMenuExpanded,
                        onDismissRequest = { seekMenuExpanded = false }
                    ) {
                        listOf(5, 10, 15, 30).forEach { sec ->
                            DropdownMenuItem(
                                text = { Text("$sec seconds") },
                                onClick = {
                                    onVideoGesturesChange(settings.videoGestureBrightness, settings.videoGestureVolume, sec)
                                    seekMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Section: Advanced & Reset
            SettingsSectionHeader(title = "Advanced & Data", icon = Icons.Default.Delete)
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDataDialog = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Clear Browsing Data", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Clear history, cache, cookies & site storage", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetDialog = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Reset All Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Restores default configuration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // About Limunix Browser Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Limunix Browser", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Version 1.0 • Built with Jetpack Compose & Media3", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Browsing Data") },
            text = { Text("Are you sure you want to clear browsing history, cookies, and cached files?") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBrowsingData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("Are you sure you want to reset all Limunix Browser settings to defaults?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSettings()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
