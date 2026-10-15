package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.LimunixTab
import com.example.repository.BoostMode
import com.example.ui.theme.LimunixBoostGreen
import com.example.ui.theme.LimunixCyan
import com.example.ui.theme.LimunixPrivatePurple
import com.example.ui.theme.LimunixWarningAmber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserMenuSheet(
    tab: LimunixTab,
    isDesktopMode: Boolean,
    isReaderAvailable: Boolean,
    isReaderActive: Boolean,
    isNightModeActive: Boolean,
    nightModeIntensity: Float,
    boostMode: BoostMode,
    dataSaverActive: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleReaderMode: () -> Unit,
    onToggleNightMode: () -> Unit,
    onNightModeIntensityChange: (Float) -> Unit,
    onToggleBoostMode: () -> Unit,
    onToggleDataSaver: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFindInPage: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onTranslatePage: () -> Unit,
    onOpenSiteSettings: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("browser_menu_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Action Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 4
            ) {
                MenuGridItem(
                    icon = Icons.Default.Add,
                    label = "New Tab",
                    onClick = { onDismiss(); onNewTab() }
                )
                MenuGridItem(
                    icon = Icons.Default.Shield,
                    label = "Private Tab",
                    iconTint = LimunixPrivatePurple,
                    onClick = { onDismiss(); onNewPrivateTab() }
                )
                MenuGridItem(
                    icon = Icons.Default.Bookmark,
                    label = "Bookmarks",
                    onClick = { onDismiss(); onOpenBookmarks() }
                )
                MenuGridItem(
                    icon = Icons.Default.History,
                    label = "History",
                    onClick = { onDismiss(); onOpenHistory() }
                )
                MenuGridItem(
                    icon = Icons.Default.Download,
                    label = "Downloads",
                    onClick = { onDismiss(); onOpenDownloads() }
                )
                MenuGridItem(
                    icon = if (isDesktopMode) Icons.Default.Smartphone else Icons.Default.Computer,
                    label = if (isDesktopMode) "Mobile Site" else "Desktop Site",
                    isActive = isDesktopMode,
                    onClick = { onToggleDesktopMode() }
                )
                MenuGridItem(
                    icon = Icons.Default.MenuBook,
                    label = "Reader Mode",
                    isEnabled = isReaderAvailable,
                    isActive = isReaderActive,
                    onClick = { onDismiss(); onToggleReaderMode() }
                )
                MenuGridItem(
                    icon = Icons.Default.FindInPage,
                    label = "Find in Page",
                    onClick = { onDismiss(); onOpenFindInPage() }
                )
                MenuGridItem(
                    icon = Icons.Default.CameraAlt,
                    label = "Screenshot",
                    onClick = { onDismiss(); onTakeScreenshot() }
                )
                MenuGridItem(
                    icon = Icons.Default.Translate,
                    label = "Translate",
                    onClick = { onDismiss(); onTranslatePage() }
                )
                MenuGridItem(
                    icon = Icons.Default.Security,
                    label = "Site Info",
                    onClick = { onDismiss(); onOpenSiteSettings() }
                )
                MenuGridItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = { onDismiss(); onOpenSettings() }
                )
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Quick Switches Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Boost Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleBoostMode)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = when (boostMode) {
                                BoostMode.MAX_BOOST -> LimunixCyan
                                BoostMode.SMART_BOOST -> LimunixBoostGreen
                                BoostMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Boost Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = when (boostMode) {
                                    BoostMode.MAX_BOOST -> "Max Boost (Strict caching & blocking)"
                                    BoostMode.SMART_BOOST -> "Smart Boost (Standard acceleration)"
                                    BoostMode.OFF -> "Off"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = when (boostMode) {
                            BoostMode.MAX_BOOST -> "MAX"
                            BoostMode.SMART_BOOST -> "SMART"
                            BoostMode.OFF -> "OFF"
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }

                // Data Saver Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Data Saver", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = "Reduces image quality & blocks auto-play",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dataSaverActive,
                        onCheckedChange = { onToggleDataSaver() }
                    )
                }

                // Night Mode / Eye Comfort Filter Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Night Mode (Eye Comfort)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = "Warm amber screen overlay",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isNightModeActive,
                        onCheckedChange = { onToggleNightMode() }
                    )
                }

                if (isNightModeActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Intensity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = nightModeIntensity,
                            onValueChange = onNightModeIntensityChange,
                            valueRange = 0.05f..0.60f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun MenuGridItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isEnabled: Boolean = true,
    isActive: Boolean = false,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                else if (!isEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                else iconTint ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = if (!isEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
