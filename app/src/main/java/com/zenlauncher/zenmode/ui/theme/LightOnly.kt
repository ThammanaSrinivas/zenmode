package com.zenlauncher.zenmode.ui.theme

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Renders [content] in light mode whatever the app theme is: light resources (so
 * colorResource skips values-night) and the light ZenColors. For things that leave the
 * app as images, like the Zen Circle share card. Leaves the system bars alone.
 */
@Composable
fun LightOnly(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lightContext = remember(context, configuration) {
        val config = Configuration(configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        }
        ContextThemeWrapper(context, context.theme).apply { applyOverrideConfiguration(config) }
    }
    CompositionLocalProvider(
        LocalContext provides lightContext,
        LocalConfiguration provides lightContext.resources.configuration
    ) {
        CompositionLocalProvider(LocalZenColors provides LightZenColors, content = content)
    }
}
