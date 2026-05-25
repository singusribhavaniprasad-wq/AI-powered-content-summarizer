package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AIAppDarkColorScheme = darkColorScheme(
    primary = GlowCyan,
    secondary = TechPurple,
    tertiary = TechTeal,
    background = CosmosBg,
    surface = CosmosSurface,
    onBackground = TextWhiteState,
    onSurface = TextWhiteState,
    surfaceVariant = CosmosSurfaceVariant,
    onSurfaceVariant = TextGrayState,
    outline = BorderColorState,
    error = SoftPink
)

private val AIAppLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme for the sci-fi, futuristic AI vibe!
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our intentional cosmic identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AIAppDarkColorScheme else AIAppLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
