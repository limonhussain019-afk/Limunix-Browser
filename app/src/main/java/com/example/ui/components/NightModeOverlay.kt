package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LimunixWarningAmber

@Composable
fun NightModeOverlay(
    intensity: Float,
    modifier: Modifier = Modifier
) {
    // Transparent amber/warm eye-comfort overlay
    val alpha = intensity.coerceIn(0f, 0.7f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LimunixWarningAmber.copy(alpha = alpha))
    )
}
