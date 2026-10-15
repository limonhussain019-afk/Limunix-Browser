package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LimunixCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF81F3FE),
    secondary = LimunixAzure,
    onSecondary = Color(0xFF002B73),
    secondaryContainer = Color(0xFF0D419D),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = LimunixBoostGreen,
    background = LimunixDeepNavy,
    onBackground = Color(0xFFE2E8F0),
    surface = LimunixSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = LimunixSurfaceElevatedDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = LimunixBorderDark,
    error = LimunixErrorRed
)

private val PrivateDarkColorScheme = darkColorScheme(
    primary = LimunixPrivatePurple,
    onPrimary = Color(0xFF280D58),
    primaryContainer = Color(0xFF3F1D80),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = LimunixCyan,
    background = Color(0xFF0C0914),
    onBackground = Color(0xFFEADBFE),
    surface = LimunixPrivateSurface,
    onSurface = Color(0xFFF3EAFF),
    surfaceVariant = Color(0xFF201833),
    outline = Color(0xFF3B2F5C)
)

private val LightColorScheme = lightColorScheme(
    primary = LimunixPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = LimunixSecondaryLight,
    background = LimunixBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = LimunixSurfaceLight,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = LimunixSurfaceElevatedLight,
    onSurfaceVariant = Color(0xFF475569),
    outline = LimunixBorderLight,
    error = LimunixErrorRed
)

@Composable
fun LimunixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isPrivateMode: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isPrivateMode -> PrivateDarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain for compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LimunixTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
