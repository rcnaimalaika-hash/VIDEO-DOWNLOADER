package com.example.videodownloader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// Color schemes
// ---------------------------------------------------------------------------

private val AppLightColorScheme = lightColorScheme(
    primary          = BrandBlue,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFDCEAFF),
    background       = LightBackground,
    onBackground     = LightOnSurface,
    surface          = LightSurface,
    onSurface        = LightOnSurface,
    surfaceVariant   = LightSurface2,
    onSurfaceVariant = LightOnSurface2,
    outline          = LightDivider,
    outlineVariant   = LightDivider,
    error            = LightError,
    onError          = Color.White,
)

private val AppDarkColorScheme = darkColorScheme(
    primary          = BrandBlueDark,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF1A3A6B),
    background       = DarkBackground,
    onBackground     = DarkOnSurface,
    surface          = DarkSurface,
    onSurface        = DarkOnSurface,
    surfaceVariant   = DarkSurface2,
    onSurfaceVariant = DarkOnSurface2,
    outline          = DarkDivider,
    outlineVariant   = DarkDivider,
    error            = DarkError,
    onError          = Color.White,
)

// ---------------------------------------------------------------------------
// CompositionLocal — lets any composable read isDark without prop drilling
// ---------------------------------------------------------------------------

val LocalIsDarkTheme = compositionLocalOf { false }

// ---------------------------------------------------------------------------
// App theme
// ---------------------------------------------------------------------------

@Composable
fun VIDEODOWNLOADERTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // always false — we use our own colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // In dark theme, status bar and nav bar icons must be light (false for isAppearanceLight)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}

// ---------------------------------------------------------------------------
// Convenience helpers — use these in composables instead of hardcoded colors
// ---------------------------------------------------------------------------

object AppColors {
    // Brand
    val blue         @Composable get() = MaterialTheme.colorScheme.primary

    // Backgrounds
    val background   @Composable get() = MaterialTheme.colorScheme.background
    val surface      @Composable get() = MaterialTheme.colorScheme.surface
    val surface2     @Composable get() = MaterialTheme.colorScheme.surfaceVariant

    // Text
    val textPrimary  @Composable get() = MaterialTheme.colorScheme.onSurface
    val textSecondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    // Divider & Border
    val divider      @Composable get() = MaterialTheme.colorScheme.outline
    val cardBorder   @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2E3448) else Color(0xFFE2E6EF)

    // Nav bar background
    val navBg        @Composable get() = MaterialTheme.colorScheme.surface

    // Card background
    val card         @Composable get() = MaterialTheme.colorScheme.surface

    // Selected item highlight
    val selectedBg   @Composable get() =
        if (LocalIsDarkTheme.current) Color(0xFF1A3A6B) else Color(0xFFEEF3FF)

    val selectedText @Composable get() = MaterialTheme.colorScheme.primary

    // Dynamic helper colors for dark/light mode
    val chipBg       @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF252836) else Color(0xFFEFF1F5)
    val chipText     @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFA5ABC0) else Color(0xFF5A6172)
    val heroBannerBg @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF182030) else Color(0xFFEEF2FF)
    val errorBg      @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2E1515) else Color(0xFFFFF3F3)
    val errorBorder  @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF6B2020) else Color(0xFFFFAEAE)
    val iconContainer @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color(0xFFEEF3FF)
    val logoutRed    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFF6B6B) else Color(0xFFE53935)
    val isDark       @Composable get() = LocalIsDarkTheme.current
}

