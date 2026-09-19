package com.zenlauncher.zenmode.ui.screens

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import com.zenlauncher.zenmode.LauncherActivities
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.ZenFrostedOverlay
import com.zenlauncher.zenmode.ui.components.ZenOverlayTagline
import com.zenlauncher.zenmode.ui.components.ZenOverlayTitle
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HomeAppPickerItem(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    /** Package + activity: one package can have two launcher entries (see [LauncherActivities]). */
    val key: String = packageName
)

/** Launcher-visible apps, A–Z. Same source as the home grid in MainActivity. */
fun loadHomeAppPickerItems(context: Context): List<HomeAppPickerItem> {
    val pm = context.packageManager
    return LauncherActivities.query(pm)
        .map {
            HomeAppPickerItem(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(pm),
                key = LauncherActivities.key(it)
            )
        }
        .sortedBy { it.label.lowercase() }
}

/**
 * Picks the apps that sit on the home screen, in order, in the frosted overlay. Position in
 * [initialSelection] is the slot on home (row by row). Slots left empty fill A–Z, so choosing
 * fewer than [limit] is fine.
 *
 * Every change is handed to [onSelectionChange] straight away, so closing never loses a choice
 * and the home screen behind can reorder live.
 */
@Composable
fun HomeAppsPickerOverlay(
    visible: Boolean,
    limit: Int,
    initialSelection: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = ZenTheme.colors

    var apps by remember { mutableStateOf<List<HomeAppPickerItem>>(emptyList()) }
    var selection by remember { mutableStateOf(initialSelection.take(limit)) }
    var query by remember { mutableStateOf("") }
    var showFullHint by remember { mutableStateOf(false) }

    // The overlay stays composed while hidden, so start fresh from storage on every open.
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        selection = initialSelection.take(limit)
        query = ""
        showFullHint = false
        val loaded = withContext(Dispatchers.IO) { loadHomeAppPickerItems(context) }
        apps = loaded
        // Drop anything uninstalled since it was chosen.
        val installed = loaded.mapTo(HashSet()) { it.packageName }
        val cleaned = selection.filter { it in installed }
        if (cleaned != selection) {
            selection = cleaned
            onSelectionChange(cleaned)
        }
    }

    fun update(next: List<String>) {
        selection = next
        showFullHint = false
        onSelectionChange(next)
    }

    fun toggle(pkg: String) {
        when {
            pkg in selection -> update(selection - pkg)
            selection.size < limit -> update(selection + pkg)
            else -> {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                showFullHint = true
            }
        }
    }

    val appsByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val visibleApps = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    // Everything inside keeps a fixed footprint: the panel is pinned to full height, the hint swaps
    // in for the tagline, and results scroll inside the grid's space, so typing never resizes it.
    ZenFrostedOverlay(
        visible = visible,
        eyebrow = "Home screen settings",
        onDismiss = onDismiss,
        onBack = onBack,
        fillHeight = true
    ) {
        ZenOverlayTitle(text = "Choose your $limit apps")
        if (showFullHint) {
            Text(
                text = "All $limit spots are taken. Remove one first.",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.rsp,
                lineHeight = 24.rsp,
                color = colors.accentDeduct,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.rdp)
            )
        } else {
            ZenOverlayTagline(text = "Tap to pick. Hold and drag to reorder.")
        }

        HomeSlotGrid(
            limit = limit,
            selection = selection,
            appsByPackage = appsByPackage,
            onRemove = { toggle(it) },
            onReorder = { update(it) },
            modifier = Modifier.padding(top = 18.rdp)
        )

        Row(
            modifier = Modifier.padding(top = 16.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PickerSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.rdp))
            Text(
                text = "${selection.size}/$limit",
                fontFamily = DepartureMono,
                fontSize = 14.rsp,
                color = if (selection.size == limit) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.semantics { contentDescription = "${selection.size} of $limit chosen" }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (apps.isNotEmpty() && visibleApps.isEmpty()) {
                Text(
                    text = "No apps match \u201C${query.trim()}\u201D",
                    fontFamily = Geist,
                    fontSize = 15.rsp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.rdp)
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(vertical = 12.rdp),
                horizontalArrangement = Arrangement.spacedBy(8.rdp),
                verticalArrangement = Arrangement.spacedBy(14.rdp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = visibleApps, key = { it.key }) { app ->
                    val position = selection.indexOf(app.packageName)
                    PickerAppCell(
                        app = app,
                        position = if (position >= 0) position + 1 else null,
                        dimmed = position < 0 && selection.size >= limit,
                        onClick = { toggle(app.packageName) }
                    )
                }
            }
        }

        Column(Modifier.padding(top = 8.rdp)) {
            HorizontalDivider(thickness = 1.dp, color = colors.borderHairlineSoft)
            Row(
                modifier = Modifier.padding(top = 8.rdp, start = 12.rdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Empty spots fill in A–Z. Changes save as you tap.",
                    fontFamily = Geist,
                    fontSize = 14.rsp,
                    lineHeight = 20.rsp,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                // Always laid out, only hidden, so it appearing never shifts anything.
                val canClear = selection.isNotEmpty()
                Text(
                    text = "Clear all",
                    fontFamily = Geist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.rsp,
                    color = colors.textBrand,
                    modifier = Modifier
                        .alpha(if (canClear) 1f else 0f)
                        .clip(RoundedCornerShape(8.rdp))
                        .clickable(enabled = canClear, role = Role.Button) { update(emptyList()) }
                        .padding(horizontal = 8.rdp, vertical = 12.rdp)
                )
            }
        }
    }
}

/**
 * The home layout, one slot per spot in rows of up to 8. Tap a filled slot to remove it; hold one
 * and drag to move it. Neighbours shift as it passes them, and the new order is saved on release.
 */
@Composable
private fun HomeSlotGrid(
    limit: Int,
    selection: List<String>,
    appsByPackage: Map<String, HomeAppPickerItem>,
    onRemove: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val gap = 8.rdp
    val columns = limit.coerceAtMost(8)
    val rows = (limit + columns - 1) / columns

    // Drag handlers outlive recompositions, so read these through updated state.
    val currentSelection by rememberUpdatedState(selection)
    val currentOnReorder by rememberUpdatedState(onReorder)

    // Live order while dragging; otherwise mirrors the saved selection.
    var order by remember { mutableStateOf(selection) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    if (dragging == null && order != selection) order = selection

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val slot = (maxWidth - gap * (columns - 1)) / columns
        val pitch = with(density) { (slot + gap).toPx() }

        fun moveBy(pkg: String, delta: Offset) {
            dragOffset += delta
            val from = order.indexOf(pkg)
            if (from < 0) return
            val col = from % columns
            val row = from / columns
            val targetCol = (col + (dragOffset.x / pitch).roundToInt()).coerceIn(0, columns - 1)
            val targetRow = (row + (dragOffset.y / pitch).roundToInt()).coerceIn(0, rows - 1)
            // Only filled spots take part; you can't drop past the last chosen app.
            val to = (targetRow * columns + targetCol).coerceIn(0, order.size - 1)
            if (to != from) {
                order = order.toMutableList().apply { add(to, removeAt(from)) }
                dragOffset -= Offset(((to % columns) - col) * pitch, ((to / columns) - row) * pitch)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }

        fun endDrag() {
            dragging = null
            dragOffset = Offset.Zero
            if (order != currentSelection) currentOnReorder(order)
        }

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(columns) { col ->
                        val index = row * columns + col
                        if (index >= limit) {
                            Spacer(Modifier.size(slot))
                            return@repeat
                        }
                        val pkg = order.getOrNull(index)
                        val app = pkg?.let { appsByPackage[it] }
                        val isDragging = pkg != null && pkg == dragging
                        key(pkg ?: "empty-$index") {
                            HomeSlot(
                                position = index + 1,
                                app = app,
                                lifted = isDragging,
                                onClick = { pkg?.let(onRemove) },
                                modifier = Modifier
                                    .size(slot)
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        if (isDragging) {
                                            translationX = dragOffset.x
                                            translationY = dragOffset.y
                                            scaleX = 1.15f
                                            scaleY = 1.15f
                                        }
                                    }
                                    .then(
                                        if (pkg == null) Modifier
                                        else Modifier.pointerInput(pkg) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                    dragging = pkg
                                                    dragOffset = Offset.Zero
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    moveBy(pkg, amount)
                                                },
                                                onDragEnd = { endDrag() },
                                                onDragCancel = { endDrag() }
                                            )
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberIconBitmap(app: HomeAppPickerItem) =
    remember(app.key) { app.icon.toBitmap(width = 128, height = 128).asImageBitmap() }

@Composable
private fun HomeSlot(
    position: Int,
    app: HomeAppPickerItem?,
    lifted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(12.rdp)
    Box(
        modifier = modifier
            .then(if (lifted) Modifier.shadow(12.rdp, shape) else Modifier)
            .clip(shape)
            .then(
                if (app == null) Modifier.border(1.rdp, colors.borderOutline, shape).background(colors.surfaceSunk)
                else Modifier.clickable(onClickLabel = "Remove ${app.label}", role = Role.Button, onClick = onClick)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            Image(
                bitmap = rememberIconBitmap(app),
                contentDescription = "Spot $position: ${app.label}. Hold and drag to move.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = position.toString(),
                fontFamily = DepartureMono,
                fontSize = 12.rsp,
                color = colors.textMuted,
                modifier = Modifier.semantics { contentDescription = "Spot $position: A–Z fill" }
            )
        }
    }
}

@Composable
private fun PickerAppCell(
    app: HomeAppPickerItem,
    position: Int?,
    dimmed: Boolean,
    onClick: () -> Unit
) {
    val colors = ZenTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.rdp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { selected = position != null }
            .padding(vertical = 4.rdp)
            .alpha(if (dimmed) 0.4f else 1f)
    ) {
        Box {
            Image(
                bitmap = rememberIconBitmap(app),
                contentDescription = null,
                modifier = Modifier
                    .size(52.rdp)
                    .clip(RoundedCornerShape(14.rdp))
            )
            if (position != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.rdp, y = (-6).rdp)
                        .size(20.rdp)
                        .clip(CircleShape)
                        .background(colors.actionPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = position.toString(),
                        fontFamily = DepartureMono,
                        fontSize = 10.rsp,
                        color = colors.actionPrimaryText
                    )
                }
            }
        }
        Spacer(Modifier.height(6.rdp))
        Text(
            text = app.label,
            fontFamily = Geist,
            fontSize = 11.rsp,
            color = if (position != null) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PickerSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = ZenTheme.colors
    Row(
        modifier = modifier
            .height(44.rdp)
            .clip(CircleShape)
            .background(colors.surfaceSunk)
            .padding(horizontal = 14.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(16.rdp),
            colorFilter = ColorFilter.tint(colors.textSecondary)
        )
        Spacer(Modifier.width(10.rdp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = Geist, fontSize = 15.rsp, color = colors.textPrimary),
            cursorBrush = SolidColor(colors.textBrand),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(text = "Search apps", fontFamily = Geist, fontSize = 15.rsp, color = colors.textMuted)
                }
                inner()
            }
        )
    }
}
