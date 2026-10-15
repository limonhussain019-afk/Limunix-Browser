package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.browser.LimunixTab
import com.example.browser.UrlHelper
import com.example.ui.theme.LimunixBoostGreen
import com.example.ui.theme.LimunixCyan

@Composable
fun SiteSettingsDialog(
    tab: LimunixTab,
    isAdBlockGloballyEnabled: Boolean,
    isWhitelisted: Boolean,
    onDismiss: () -> Unit,
    onToggleWhitelist: (Boolean) -> Unit,
    onClearSiteData: () -> Unit
) {
    val domain = UrlHelper.extractDisplayDomain(tab.url)
    var isSiteAdBlockEnabled by remember { mutableStateOf(!isWhitelisted) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (tab.isSecure) Icons.Default.Lock else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (tab.isSecure) LimunixCyan else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(domain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = if (tab.isSecure) "Connection is secure (HTTPS)" else "Insecure connection (HTTP)",
                        fontSize = 12.sp,
                        color = if (tab.isSecure) LimunixBoostGreen else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Divider()

                // Ad Blocker for this site
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block Ads & Trackers", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (isSiteAdBlockEnabled) "Active on this site" else "Paused for this site",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isSiteAdBlockEnabled,
                        onCheckedChange = { checked ->
                            isSiteAdBlockEnabled = checked
                            onToggleWhitelist(!checked)
                        }
                    )
                }

                // Desktop site state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Desktop Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        text = if (tab.isDesktopMode) "Enabled" else "Mobile",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Clear Site Data Button
                Button(
                    onClick = {
                        onClearSiteData()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Cookies & Cache for Site")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        modifier = Modifier.testTag("site_settings_dialog")
    )
}
