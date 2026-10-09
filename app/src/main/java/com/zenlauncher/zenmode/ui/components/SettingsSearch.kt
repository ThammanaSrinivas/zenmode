package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * Search across Settings, filtering the screen in place rather than keeping a second list of
 * every setting: each row ([ZenSettingsRow], [ZenSettingToggleItem], and any custom row that
 * calls [settingsRowVisible]) hides itself when it doesn't match, and a [ZenSettingsGroup] left
 * with no rows folds away, label and all. A row's words are its title, its subtitle and the name
 * of its group — so "phone" lists the whole Phone group. Outside Settings the query is empty and
 * nothing changes.
 */
val LocalSettingsQuery = compositionLocalOf { "" }

/** The group a row sits in, so a query naming the group finds every row in it. */
internal val LocalSettingsGroupLabel = compositionLocalOf { "" }

/** True when every word of [query] appears somewhere in [texts], ignoring case. Pure, for tests. */
fun settingsMatch(query: String, vararg texts: String?): Boolean {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val haystack = texts.filterNotNull().joinToString(" ").lowercase()
    return words.all { it in haystack }
}

/** Whether a row with this [title] and [subtitle] shows under the current search. */
@Composable
fun settingsRowVisible(title: String, subtitle: String? = null): Boolean =
    settingsMatch(LocalSettingsQuery.current, title, subtitle, LocalSettingsGroupLabel.current)

/** True while Settings is filtering. */
@Composable
fun isSettingsSearching(): Boolean = LocalSettingsQuery.current.isNotBlank()

@Composable
fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.rdp)
            .clip(shape)
            .border(1.dp, colors.borderOutline, shape)
            .padding(start = 16.rdp, end = 4.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.rdp))
        Spacer(Modifier.width(10.rdp))
        Box(Modifier.weight(1f)) {
            val style = TextStyle(fontFamily = Geist, fontSize = 15.rsp, color = colors.textPrimary)
            if (query.isEmpty()) Text("Search settings", style = style.copy(color = colors.textMuted))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(colors.textBrand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.rdp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = colors.textSecondary, modifier = Modifier.size(18.rdp))
            }
        }
    }
}
