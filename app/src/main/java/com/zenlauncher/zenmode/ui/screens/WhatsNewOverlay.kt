package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.WhatsNew
import com.zenlauncher.zenmode.WhatsNewItem
import com.zenlauncher.zenmode.ui.components.ZenButton
import com.zenlauncher.zenmode.ui.components.ZenFrostedOverlay
import com.zenlauncher.zenmode.ui.components.ZenModeOsWordmark
import com.zenlauncher.zenmode.ui.components.ZenOverlayFootnote
import com.zenlauncher.zenmode.ui.components.ZenOverlayTagline
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * The release notes, once, after an update: the same frosted panel as every other Home
 * overlay, so it reads as part of the launcher rather than a system dialog. The wordmark
 * carries the headline; each item is a dot, a name and one line. Close, back, a tap outside
 * and the button all mean "seen".
 */
@Composable
fun WhatsNewOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    versionName: String,
    items: List<WhatsNewItem> = WhatsNew.items
) {
    ZenFrostedOverlay(visible = visible, eyebrow = "What's new · v$versionName", onDismiss = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 6.rdp)
                    .staggeredEntrance(0)
                    .semantics(mergeDescendants = true) { heading() }
            ) {
                ZenModeOsWordmark(fontSize = 26.rsp)
                Text(
                    text = " is here",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 26.rsp,
                    color = ZenTheme.colors.textPrimary
                )
            }
            Box(Modifier.staggeredEntrance(1)) { ZenOverlayTagline(text = WhatsNew.HEADLINE) }
            Spacer(Modifier.height(14.rdp))
            items.forEachIndexed { i, item ->
                WhatsNewRow(item, Modifier.staggeredEntrance(2 + i))
            }
            Spacer(Modifier.height(18.rdp))
            ZenButton(
                text = "Let's go",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().staggeredEntrance(3 + items.size)
            )
            Spacer(Modifier.height(8.rdp))
            ZenOverlayFootnote(text = "Thanks for using $PRODUCT_NAME 🤍")
        }
    }
}

@Composable
private fun WhatsNewRow(item: WhatsNewItem, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.rdp)
            .semantics(mergeDescendants = true) {}
    ) {
        Box(Modifier.size(24.rdp, 22.rdp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(8.rdp)
                    .clip(CircleShape)
                    .background(if (item.isReward) colors.accentRewardGraphic else colors.textBrand)
            )
        }
        Spacer(Modifier.width(12.rdp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.rsp,
                lineHeight = 22.rsp,
                color = colors.textPrimary
            )
            Text(
                text = item.detail,
                fontFamily = Geist,
                fontSize = 13.rsp,
                lineHeight = 18.rsp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.rdp)
            )
        }
    }
}
