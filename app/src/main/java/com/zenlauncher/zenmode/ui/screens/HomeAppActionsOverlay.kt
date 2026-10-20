package com.zenlauncher.zenmode.ui.screens

import android.widget.ImageView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.ui.components.HomeGridGlyph
import com.zenlauncher.zenmode.ui.components.InfoGlyph
import com.zenlauncher.zenmode.ui.components.ZenFrostedOverlay
import com.zenlauncher.zenmode.ui.components.ZenOverlayAction
import com.zenlauncher.zenmode.ui.components.ZenOverlayFootnote
import com.zenlauncher.zenmode.ui.components.ZenOverlayTagline
import com.zenlauncher.zenmode.ui.components.ZenOverlayTitle
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.R
import androidx.compose.ui.res.stringResource

/**
 * What a long-press on a home app opens: a frosted overlay with the app's two actions.
 * [app] null means closed. [position] is its 1-based spot on home.
 */
@Composable
fun HomeAppActionsOverlay(
    app: AppInfo?,
    position: Int,
    appCount: Int,
    onChangeHomeApps: () -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    // Keep the last app while the overlay animates out, so the panel doesn't go blank.
    val lastApp = remember { arrayOfNulls<AppInfo>(1) }
    if (app != null) lastApp[0] = app
    val current = lastApp[0] ?: return

    ZenFrostedOverlay(visible = app != null, eyebrow = stringResource(R.string.common_home_screen_settings), onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AndroidView(
                factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                update = { it.setImageDrawable(current.icon) },
                modifier = Modifier
                    .size(36.rdp)
                    .clip(RoundedCornerShape(10.rdp))
            )
            Spacer(Modifier.width(12.rdp))
            ZenOverlayTitle(text = current.label.toString())
        }
        ZenOverlayTagline(
            text = if (position in 1..appCount) stringResource(R.string.home_app_actions_tagline_spot, position, appCount)
            else stringResource(R.string.home_app_actions_tagline_one_of, appCount)
        )

        Spacer(Modifier.padding(top = 10.rdp))

        ZenOverlayAction(
            title = stringResource(R.string.home_app_actions_change_apps_title),
            subtitle = stringResource(R.string.home_app_actions_change_apps_subtitle, appCount),
            onClick = onChangeHomeApps,
            icon = { HomeGridGlyph() }
        )
        ZenOverlayAction(
            title = stringResource(R.string.home_app_actions_app_info_title),
            subtitle = stringResource(R.string.home_app_actions_app_info_subtitle),
            onClick = { onAppInfo(current) },
            icon = { InfoGlyph() }
        )

        ZenOverlayFootnote(text = stringResource(R.string.home_app_actions_footnote))
    }
}
