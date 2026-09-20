package com.zenlauncher.zenmode.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.BuildConfig
import com.zenlauncher.zenmode.ContentBlockPrefs
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.ZenModeOsSettingsTitle
import com.zenlauncher.zenmode.ui.components.ZenSwitch
import com.zenlauncher.zenmode.ui.components.zenToggleable
import com.zenlauncher.zenmode.ui.components.dropShadow
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── Settings / Distraction Blocker ───────────────────────────────
// Figma nodes 2026:949 (Pause 30m) and 2026:992 (Paused 30m, reels off). One green card for
// the short-form feeds, then every app on the phone with its own "quiet" switch.

data class BlockerApp(
    val packageName: String,
    val label: String,
    val minutesToday: Long,
    /** In-app surfaces this app has in the block rules (id to label), e.g. Instagram Reels. */
    val surfaces: List<Pair<String, String>> = emptyList()
)

data class BlockerState(
    val apps: List<BlockerApp> = emptyList(),
    val appsLoaded: Boolean = false,
    val quieted: Set<String> = emptySet(),
    /** "<package>/<surfaceId>" for every blocked surface. */
    val blockedSurfaces: Set<String> = emptySet(),
    val reelsQuieted: Boolean = false,
    val pausedUntil: Long = 0L,
    val stops: Int = 0,
    val minutesSaved: Int = 0,
    val accessibilityOn: Boolean = true,
    val isPro: Boolean = false,
    val debugDump: Boolean = false,
    val lastCrash: String? = null
) {
    val isPaused: Boolean get() = pausedUntil > System.currentTimeMillis()
}

private enum class AppFilter(val label: String) { ALL("All"), QUIETED("Quieted"), OPEN("Open") }

private val ScreenMargin: Dp @Composable get() = 30.rdp

@Composable
fun DistractionBlockerScreen(
    state: BlockerState,
    onBackClick: () -> Unit,
    onPauseClick: () -> Unit,
    onReelsToggle: (Boolean) -> Unit,
    onAppToggle: (packageName: String, quieted: Boolean) -> Unit,
    onSurfaceToggle: (packageName: String, surfaceId: String, blocked: Boolean) -> Unit,
    onEnableAccessibility: () -> Unit,
    onDebugDumpToggle: (Boolean) -> Unit = {},
    onClearLastCrash: () -> Unit = {}
) {
    val colors = ZenTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(AppFilter.ALL) }

    val searched = remember(state.apps, query) {
        if (query.isBlank()) state.apps
        else state.apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val quietedCount = searched.count { it.packageName in state.quieted }
    val shown = when (filter) {
        AppFilter.ALL -> searched
        AppFilter.QUIETED -> searched.filter { it.packageName in state.quieted }
        AppFilter.OPEN -> searched.filterNot { it.packageName in state.quieted }
    }

    // Plain page, not the mood backdrop: the green card and app list read best on neutral paper.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 96.rdp)
        ) {
            item {
                Spacer(Modifier.height(20.rdp))
                BlockerHeader(state = state, onBackClick = onBackClick, onPauseClick = onPauseClick)
                Spacer(Modifier.height(28.rdp))
                Column(Modifier.padding(horizontal = ScreenMargin)) {
                    Text(
                        text = "Distraction Blocker",
                        fontFamily = ClashDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 34.rsp,
                        letterSpacing = (-0.72).sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Choose what stays, and what quietly goes away.",
                        fontFamily = Geist,
                        fontSize = 14.rsp,
                        letterSpacing = (-0.28).sp,
                        color = colors.textSecondary
                    )
                }
                if (!state.accessibilityOn) {
                    Spacer(Modifier.height(16.rdp))
                    AccessibilityBanner(onClick = onEnableAccessibility)
                }
                Spacer(Modifier.height(20.rdp))
                QuietReelsCard(
                    on = state.reelsQuieted,
                    paused = state.isPaused,
                    stops = state.stops,
                    minutesSaved = state.minutesSaved,
                    onToggle = onReelsToggle
                )
                Spacer(Modifier.height(24.rdp))
                AppSearchField(query = query, onQueryChange = { query = it })
                Spacer(Modifier.height(14.rdp))
                Row(
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                    horizontalArrangement = Arrangement.spacedBy(9.rdp)
                ) {
                    AppFilter.entries.forEach { f ->
                        val count = when (f) {
                            AppFilter.ALL -> searched.size
                            AppFilter.QUIETED -> quietedCount
                            AppFilter.OPEN -> searched.size - quietedCount
                        }
                        FilterChip(label = f.label, count = count, selected = filter == f, onClick = { filter = f })
                    }
                }
                Spacer(Modifier.height(16.rdp))
            }

            when {
                !state.appsLoaded -> item {
                    Box(Modifier.fillMaxWidth().padding(24.rdp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.textBrand, strokeWidth = 2.dp, modifier = Modifier.size(22.rdp))
                    }
                }
                shown.isEmpty() -> item { EmptyApps(filter = filter, searching = query.isNotBlank()) }
                else -> items(shown, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        quieted = app.packageName in state.quieted,
                        blockedSurfaces = state.blockedSurfaces,
                        isFirst = app == shown.first(),
                        isLast = app == shown.last(),
                        onToggle = { onAppToggle(app.packageName, it) },
                        onSurfaceToggle = { id, on -> onSurfaceToggle(app.packageName, id, on) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                item {
                    DebugTools(
                        debugDump = state.debugDump,
                        lastCrash = state.lastCrash,
                        onDebugDumpToggle = onDebugDumpToggle,
                        onClearLastCrash = onClearLastCrash
                    )
                }
            }
        }

        // The design's frosted foot: the list fades out under the gesture bar.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.rdp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bgPrimary)))
                .navigationBarsPadding()
        )
    }
}

// ── Header ────────────────────────────────────────────────────────

@Composable
private fun BlockerHeader(state: BlockerState, onBackClick: () -> Unit, onPauseClick: () -> Unit) {
    val green = colorResource(R.color.gold_delta_text)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.rdp))
                .clickable(onClickLabel = "Back to settings", onClick = onBackClick)
                .padding(vertical = 6.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                colorFilter = ColorFilter.tint(green),
                modifier = Modifier.size(28.rdp)
            )
            Spacer(Modifier.width(10.rdp))
            ZenModeOsSettingsTitle(fontSize = 18.rsp, color = green, letterSpacing = (-0.36).sp)
        }
        Spacer(Modifier.weight(1f))
        PausePill(paused = state.isPaused, pausedUntil = state.pausedUntil, isPro = state.isPro, onClick = onPauseClick)
    }
}

/** "Pause 30m" (dark, PRO) → tap → "Paused 24m" (light green, counting down); tap again to resume. */
@Composable
private fun PausePill(paused: Boolean, pausedUntil: Long, isPro: Boolean, onClick: () -> Unit) {
    val colors = ZenTheme.colors
    val green = colorResource(R.color.gold_delta_text)
    Box {
        Box(
            modifier = Modifier
                .height(34.rdp)
                .clip(CircleShape)
                .then(
                    if (paused) Modifier
                        .background(colors.surfaceTint)
                        .border(1.dp, green, CircleShape)
                    else Modifier.background(colors.actionPrimary)
                )
                .pressScale(onClick = onClick, onClickLabel = if (paused) "Resume blocking" else "Pause blocking for 30 minutes")
                .padding(horizontal = 18.rdp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (paused) "Paused ${minutesLeft(pausedUntil)}m" else "Pause ${ContentBlockPrefs.PAUSE_MINUTES}m",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 14.rsp,
                letterSpacing = (-0.28).sp,
                color = if (paused) green else colors.actionPrimaryText,
                maxLines = 1
            )
        }
        if (!paused && !isPro) {
            ProTag(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).rdp, y = 6.rdp)
            )
        }
    }
}

private fun minutesLeft(until: Long): Long =
    TimeUnit.MILLISECONDS.toMinutes(until - System.currentTimeMillis()).coerceAtLeast(0) + 1

@Composable
private fun ProTag(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colorResource(R.color.gold_delta_text))
            .padding(horizontal = 3.rdp, vertical = 1.rdp)
    ) {
        Text("PRO", fontFamily = Geist, fontSize = 5.6.rsp, lineHeight = 7.rsp, color = Color.White)
    }
}

@Composable
private fun AccessibilityBanner(onClick: () -> Unit) {
    val colors = ZenTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.rdp))
            .background(colorResource(R.color.amber_500).copy(alpha = 0.16f))
            .clickable(onClickLabel = "Open accessibility settings", onClick = onClick)
            .padding(14.rdp)
    ) {
        Text(
            text = "Turn on ZenMode in Accessibility",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.rsp,
            color = colors.textPrimary
        )
        Text(
            text = "Quieting needs it to spot a feed or an app opening. Nothing is blocked until you do. Tap to open.",
            fontFamily = Geist,
            fontSize = 12.rsp,
            lineHeight = 16.rsp,
            color = colors.textSecondary
        )
    }
}

// ── Quiet reels & shorts ─────────────────────────────────────────

@Composable
private fun QuietReelsCard(
    on: Boolean,
    paused: Boolean,
    stops: Int,
    minutesSaved: Int,
    onToggle: (Boolean) -> Unit
) {
    val cardGreen = colorResource(R.color.gold_delta_text)
    val soft = colorResource(R.color.distraction_card_text)
    val radius = 24.rdp
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .dropShadow(color = Color.Black.copy(alpha = 0.13f), blur = 24.rdp, cornerRadius = radius, offsetY = 7.rdp)
            .clip(RoundedCornerShape(radius))
            .background(cardGreen)
            .zenToggleable(value = on, onValueChange = onToggle)
            .padding(horizontal = 26.rdp, vertical = 24.rdp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Quiet reels & shorts",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 23.rsp,
                    letterSpacing = (-0.48).sp,
                    color = soft,
                    maxLines = 1
                )
                Text(
                    text = when {
                        paused -> "Paused. Feeds are open until the pause ends."
                        on -> "Break the endless scrolling, right now."
                        else -> "Off. Reels, Shorts and Spotlight scroll freely."
                    },
                    fontFamily = Geist,
                    fontSize = 12.rsp,
                    letterSpacing = (-0.24).sp,
                    color = soft
                )
            }
            Spacer(Modifier.width(12.rdp))
            ZenSwitch(
                checked = on,
                onTrack = colorResource(R.color.zen_300),
                offTrack = Color.White.copy(alpha = 0.3f),
                outlineWhenOff = false
            )
        }
        Spacer(Modifier.height(16.rdp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.25f)))
        Spacer(Modifier.height(14.rdp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stat(label = "Approx scrolls stopped", value = "~$stops", unit = null, modifier = Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(46.rdp).background(Color.White.copy(alpha = 0.25f)))
            val (value, unit) = savedParts(minutesSaved)
            Stat(label = "Time saved quieting", value = "~$value", unit = unit, modifier = Modifier.weight(1f).padding(start = 16.rdp))
        }
    }
}

/** 420 → ("7", "HOURS"); 40 → ("40", "MINS"). */
private fun savedParts(minutes: Int): Pair<String, String> = when {
    minutes >= 60 -> (minutes / 60).toString() to if (minutes / 60 == 1) "HOUR" else "HOURS"
    else -> minutes.toString() to "MINS"
}

@Composable
private fun Stat(label: String, value: String, unit: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 10.rsp,
            letterSpacing = (-0.2).sp,
            color = Color.White,
            maxLines = 1
        )
        Spacer(Modifier.height(2.rdp))
        Text(
            text = buildAnnotatedString {
                append(value)
                if (unit != null) withStyle(SpanStyle(fontSize = 14.rsp)) { append(unit) }
            },
            fontFamily = DepartureMono,
            fontSize = 32.rsp,
            letterSpacing = (-0.64).sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

// ── Search + filters ─────────────────────────────────────────────

@Composable
private fun AppSearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = ZenTheme.colors
    val style = TextStyle(fontFamily = Geist, fontSize = 16.rsp, letterSpacing = (-0.16).sp, color = colors.textPrimary)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(colors.textBrand),
        modifier = Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .height(46.rdp)
            .clip(CircleShape)
            .border(1.25.dp, colorResource(R.color.gold_delta_text).copy(alpha = 0.7f), CircleShape),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.padding(horizontal = 22.rdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_search_v3),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.textSecondary),
                    modifier = Modifier.size(19.rdp)
                )
                Spacer(Modifier.width(12.rdp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search apps", style = style.copy(color = colors.textSecondary), maxLines = 1)
                    inner()
                }
            }
        }
    )
}

@Composable
private fun FilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = ZenTheme.colors
    val green = colorResource(R.color.gold_delta_text)
    Row(
        modifier = Modifier
            .height(34.rdp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(colors.actionPrimary)
                else Modifier.background(colors.surfaceElevated).border(1.dp, green, CircleShape)
            )
            .zenToggleable(value = selected, onValueChange = { onClick() }, role = Role.Tab)
            .padding(horizontal = 18.rdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.rdp)
    ) {
        val content = if (selected) colors.actionPrimaryText else colors.textSecondary
        Text(label, fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.rsp, color = content)
        Text(String.format(Locale.US, "%02d", count), fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 15.rsp, color = content)
    }
}

// ── App rows ─────────────────────────────────────────────────────

@Composable
private fun AppRow(
    app: BlockerApp,
    quieted: Boolean,
    blockedSurfaces: Set<String>,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onSurfaceToggle: (surfaceId: String, blocked: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val hasSurfaces = app.surfaces.isNotEmpty()
    var expanded by rememberSaveable(app.packageName) { mutableStateOf(false) }
    val chevronTurn by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 12.rdp else 0.dp,
        topEnd = if (isFirst) 12.rdp else 0.dp,
        bottomStart = if (isLast) 12.rdp else 0.dp,
        bottomEnd = if (isLast) 12.rdp else 0.dp
    )

    Column(
        modifier = modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceElevated)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.rdp)
                .zenToggleable(value = quieted, onValueChange = onToggle)
                .padding(horizontal = 10.rdp, vertical = 12.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconTile(packageName = app.packageName, label = app.label)
            Spacer(Modifier.width(14.rdp))
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.rdp))
                        .then(
                            if (hasSurfaces) Modifier.clickable(onClickLabel = if (expanded) "Hide in-app feeds" else "Show in-app feeds") { expanded = !expanded }
                            else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.label,
                        fontFamily = Geist,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.rsp,
                        letterSpacing = (-0.16).sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (hasSurfaces) {
                        Spacer(Modifier.width(6.rdp))
                        Text(
                            text = "›",
                            fontFamily = Geist,
                            fontSize = 16.rsp,
                            color = colors.textSecondary,
                            modifier = Modifier.rotate(chevronTurn)
                        )
                    }
                }
                Text(
                    text = usageLabel(app.minutesToday),
                    fontFamily = DepartureMono,
                    fontSize = 12.rsp,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.width(12.rdp))
            ZenSwitch(checked = quieted, onTrack = colorResource(R.color.distraction_switch_on))
        }

        // In-app feeds this app has (Instagram Reels, YouTube Shorts…), for quieting just those.
        AnimatedVisibility(
            visible = expanded && hasSurfaces,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(Modifier.padding(start = 64.rdp, end = 10.rdp, bottom = 10.rdp)) {
                app.surfaces.forEach { (id, label) ->
                    val blocked = "${app.packageName}/$id" in blockedSurfaces
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.rdp))
                            .zenToggleable(value = blocked, onValueChange = { onSurfaceToggle(id, it) })
                            .padding(vertical = 8.rdp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quiet $label only",
                            fontFamily = Geist,
                            fontSize = 14.rsp,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ZenSwitch(checked = blocked, onTrack = colorResource(R.color.distraction_switch_on))
                    }
                }
            }
        }

        if (!isLast) {
            Box(
                Modifier
                    .padding(horizontal = 12.rdp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.borderSubtle)
            )
        }
    }
}

/**
 * The app's real launcher icon, in the design's 40dp rounded tile. Icons are decoded off the
 * main thread (a long app list would otherwise stutter while scrolling); until one is ready,
 * or if the app has none, the tile shows the design's two-letter initials instead.
 */
@Composable
private fun AppIconTile(packageName: String, label: String) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { 40.rdp.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).asImageBitmap()
            }.getOrNull()
        }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(40.rdp)
                .clip(RoundedCornerShape(10.rdp))
        )
    } else {
        InitialsTile(label)
    }
}

/** "Ig", "Yt", "Rd" — the design's two-letter tile, shown while the real icon loads. */
@Composable
private fun InitialsTile(label: String) {
    Box(
        modifier = Modifier
            .size(40.rdp)
            .clip(RoundedCornerShape(10.rdp))
            .background(ZenTheme.colors.surfaceTint),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsFor(label),
            fontFamily = DepartureMono,
            fontSize = 13.rsp,
            color = colorResource(R.color.gold_delta_text)
        )
    }
}

internal fun initialsFor(label: String): String {
    val words = label.split(' ', '-', '_').filter { it.isNotBlank() }
    // Two words, or one CamelCase word ("YouTube"), use the start of each part.
    val parts = if (words.size >= 2) words else label.split(Regex("(?<=[a-z])(?=[A-Z])"))
    val letters = if (parts.size >= 2) "${parts[0].first()}${parts[1].first()}"
    else label.filter { it.isLetterOrDigit() }.let { it.take(1) + (it.drop(1).firstOrNull { c -> c.lowercaseChar() !in "aeiou" } ?: it.getOrNull(1) ?: "") }
    return letters.lowercase().replaceFirstChar { it.uppercase() }
}

private fun usageLabel(minutes: Long): String = when {
    minutes <= 0 -> "not opened today"
    minutes < 60 -> "${minutes}m today"
    else -> "${minutes / 60}h ${minutes % 60}m today"
}

@Composable
private fun EmptyApps(filter: AppFilter, searching: Boolean) {
    Text(
        text = when {
            searching -> "No apps match that search."
            filter == AppFilter.QUIETED -> "Nothing quieted yet. Flip an app's switch to quiet it."
            else -> "Every app is quieted."
        },
        fontFamily = Geist,
        fontSize = 14.rsp,
        color = ZenTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 20.rdp)
    )
}

// ── Debug (debug builds only) ────────────────────────────────────

@Composable
private fun DebugTools(
    debugDump: Boolean,
    lastCrash: String?,
    onDebugDumpToggle: (Boolean) -> Unit,
    onClearLastCrash: () -> Unit
) {
    val colors = ZenTheme.colors
    Column(Modifier.padding(horizontal = ScreenMargin).padding(top = 24.rdp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .zenToggleable(value = debugDump, onValueChange = onDebugDumpToggle)
                .padding(vertical = 8.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Log view IDs (debug)", fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.rsp, color = colors.textPrimary)
                Text(
                    "Writes on-screen view IDs to logcat (tag ZenA11y) to tune the feed rules.",
                    fontFamily = Geist,
                    fontSize = 12.rsp,
                    color = colors.textSecondary
                )
            }
            ZenSwitch(checked = debugDump)
        }
        if (lastCrash != null) {
            Text(
                text = "Blocker last crashed: $lastCrash. Tap to clear.",
                fontFamily = Geist,
                fontSize = 12.rsp,
                color = colors.textSecondary,
                modifier = Modifier
                    .clickable(onClick = onClearLastCrash)
                    .padding(vertical = 8.rdp)
            )
        }
    }
}
