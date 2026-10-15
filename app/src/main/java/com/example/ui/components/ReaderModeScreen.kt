package com.example.ui.components

import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.ReaderArticle
import com.example.browser.ReaderTheme

@Composable
fun ReaderModeScreen(
    article: ReaderArticle,
    theme: ReaderTheme,
    fontSize: Int,
    onClose: () -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onFontSizeChange: (Int) -> Unit
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    val bgColor = Color(theme.bgHex)
    val textColor = Color(theme.textHex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("reader_mode_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                color = bgColor,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Reader Mode", tint = textColor)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Font Size Decrease
                        IconButton(onClick = { onFontSizeChange(-2) }) {
                            Text("A-", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Font Size Increase
                        IconButton(onClick = { onFontSizeChange(2) }) {
                            Text("A+", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        // Theme Selector
                        Box {
                            IconButton(onClick = { showThemeMenu = true }) {
                                Icon(Icons.Default.Palette, contentDescription = "Reader Theme", tint = textColor)
                            }
                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false }
                            ) {
                                ReaderTheme.values().forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.label) },
                                        onClick = {
                                            onThemeChange(t)
                                            showThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = textColor.copy(alpha = 0.15f))

            // Article Content Scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Article Title
                Text(
                    text = article.title,
                    fontSize = (fontSize + 6).sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = (fontSize + 12).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata (Read time & word count)
                Text(
                    text = "${article.estimatedMinutes} min read • ${article.wordCount} words",
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Formatted HTML Content rendered in TextView
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(theme.textHex.toInt())
                            textSize = fontSize.toFloat()
                            setLineSpacing(8f, 1.3f)
                        }
                    },
                    update = { textView ->
                        textView.setTextColor(theme.textHex.toInt())
                        textView.textSize = fontSize.toFloat()
                        textView.text = Html.fromHtml(article.contentHtml, Html.FROM_HTML_MODE_LEGACY)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
