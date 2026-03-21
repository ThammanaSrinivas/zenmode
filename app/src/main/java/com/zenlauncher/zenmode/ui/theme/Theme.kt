package com.zenlauncher.zenmode.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = ZenBase,
    background = Black,
    onBackground = White,
    surface = Grey800,
    onSurface = Grey400
)

private val LightColorScheme = lightColorScheme(
    primary = ZenDark,
    background = White,
    onBackground = Black,
    surface = Grey100,
    onSurface = Grey600
)

@Composable
fun ZenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Dynamic color is disabled by default for ZenLauncher styling
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val zenColors = if (darkTheme) DarkZenColors else LightZenColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // In a real launcher you might want a transparent status bar, adjust as needed!
        }
    }

    // Wrap Material theme with our custom CompositionLocal
    CompositionLocalProvider(
        LocalZenColors provides zenColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
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
