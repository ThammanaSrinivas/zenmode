package com.zenlauncher.zenmode.ui.theme

import android.app.Activity
import android.os.Build
import com.zenlauncher.zenmode.ThemeMode
import com.zenlauncher.zenmode.ThemePreferences
import com.zenlauncher.zenmode.ui.components.ZenMotion
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Whether ink should render, live: recomposes the moment the Appearance setting changes so an
 * open screen can crossfade instead of waiting for AppCompat to recreate it.
 */
@Composable
fun rememberDarkTheme(): Boolean {
    val context = LocalContext.current
    val mode by remember(context) { ThemePreferences.modeState(context) }.collectAsState()
    return when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        // The activity's own configuration still says whatever AppCompat forced; ask the system.
        ThemeMode.SYSTEM -> remember(mode, LocalConfiguration.current) { ThemePreferences.isSystemDark() }
    }
}

@Composable
fun ZenTheme(
    darkTheme: Boolean = rememberDarkTheme(),
    dynamicColor: Boolean = false, // Dynamic color is disabled by default for ZenLauncher styling
    content: @Composable () -> Unit
) {
    // Paper ↔ ink is a 420ms crossfade of every token rather than a cut. First composition
    // lands directly on the right theme; only a change while on screen animates.
    val inkAmount by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(ZenMotion.SLOW, easing = FastOutSlowInEasing),
        label = "themeCrossfade"
    )
    val zenColors = when (inkAmount) {
        0f -> LightZenColors
        1f -> DarkZenColors
        else -> lerp(LightZenColors, DarkZenColors, inkAmount)
    }

    val darkColorScheme = darkColorScheme(
        primary = zenColors.textBrand,
        background = zenColors.bgPrimary,
        onBackground = zenColors.textPrimary,
        surface = zenColors.bgSecondary,
        onSurface = zenColors.textSecondary
    )

    val lightColorScheme = lightColorScheme(
        primary = zenColors.textBrand,
        background = zenColors.bgPrimary,
        onBackground = zenColors.textPrimary,
        surface = zenColors.bgSecondary,
        onSurface = zenColors.textSecondary
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Safe cast: view.context is always a real Activity in the running app, but
            // screenshot-test rendering (Paparazzi) provides a LayoutLib BridgeContext
            // instead, which would otherwise crash this cast.
            (view.context as? Activity)?.let { activity ->
                val window = activity.window
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Wrap Material theme with our custom CompositionLocal
    CompositionLocalProvider(
        LocalZenColors provides zenColors
    ) {
        ProvideScreenScale {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}

// Accessor for the custom design tokens
object ZenTheme {
    val colors: ZenColors
        @Composable
        get() = LocalZenColors.current
    
    val typography: ZenTypography
        get() = ZenTypography
}
