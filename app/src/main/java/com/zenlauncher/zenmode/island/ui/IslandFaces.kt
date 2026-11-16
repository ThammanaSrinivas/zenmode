package com.zenlauncher.zenmode.island.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.coreapi.week.MomentFeeling
import com.zenlauncher.zenmode.island.CallItem
import com.zenlauncher.zenmode.island.ChargingPeek
import com.zenlauncher.zenmode.island.IslandAction
import com.zenlauncher.zenmode.island.LastHourItem
import com.zenlauncher.zenmode.island.LiveItem
import com.zenlauncher.zenmode.island.MediaItem
import com.zenlauncher.zenmode.island.MomentItem
import com.zenlauncher.zenmode.island.NotePeek
import com.zenlauncher.zenmode.island.PromisePeek
import com.zenlauncher.zenmode.island.QuietDonePeek
import com.zenlauncher.zenmode.island.QuietItem
import com.zenlauncher.zenmode.ui.theme.IslandPalette
import com.zenlauncher.zenmode.widget.art.ZenoSkin
import java.text.DateFormat
import java.util.Date

// What each thing looks like in the island: in the pill (something left of the camera, something
// right of it), as the small bubble beside the pill, and as the open card. The card keeps a band
// at its top as tall as the pill, empty in the middle, so nothing is ever drawn under the camera.

/** One thing in the pill: [lead] left of the camera, [trail] right of it, and what TalkBack says. */
internal class Compact(
    val leadWidth: Dp,
    val trailWidth: Dp,
    val description: String,
    val lead: @Composable () -> Unit,
    val trail: @Composable () -> Unit
)

private val TRAIL_MAX = 112.dp

@Composable
private fun widthOf(text: String, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val px = remember(text, style) { measurer.measure(text, style, maxLines = 1).size.width }
    return with(density) { px.toDp() }.coerceAtMost(TRAIL_MAX) + 1.dp
}

@Composable
private fun TrailText(text: String, style: TextStyle, color: Color) {
    Text(text, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun compactFor(item: Any, now: Long, lite: Boolean): Compact? = when (item) {
    is QuietItem -> {
        val t = IslandTime.clock(item.endsAt - now)
        Compact(22.dp, widthOf(t, IslandType.number), "Quiet session, $t left", { MoonGlyph(Modifier.size(22.dp)) }) {
            TrailText(t, IslandType.number, IslandPalette.zen)
        }
    }
    is CallItem -> {
        val t = if (item.incoming) item.name else IslandTime.clock(now - item.since)
        val style = if (item.incoming) IslandType.word else IslandType.number
        Compact(18.dp, widthOf(t, style), if (item.incoming) "Call from ${item.name}" else "On a call, $t", { PhoneGlyph(Modifier.size(17.dp)) }) {
            TrailText(t, style, if (item.incoming) IslandPalette.text else IslandPalette.zen)
        }
    }
    is LiveItem -> {
        val t = liveValue(item, now)
        val style = if (item.chronoBase != null || item.progress != null) IslandType.number else IslandType.word
        Compact(20.dp, widthOf(t, style), "${item.appLabel}: ${item.title}", { Picture(item.icon, 20.dp, 6.dp) { Monogram(item.appLabel, 20.dp) } }) {
            TrailText(t, style, IslandPalette.text)
        }
    }
    is MediaItem -> Compact(22.dp, 20.dp, "${item.title}, ${item.artist}", {
        Picture(item.art, 22.dp, 6.dp) { MusicBars(false, Color(item.tint), true, Modifier.size(14.dp)) }
    }) {
        MusicBars(item.playing, Color(item.tint), lite, Modifier.size(width = 20.dp, height = 14.dp))
    }
    is LastHourItem -> {
        val t = IslandTime.minutes(item.minutesLeft)
        Compact(24.dp, widthOf(t, IslandType.number), MomentCopy.lastHour(item.minutesLeft), {
            ZenoGlyph(MomentFeeling.ALERT, Modifier.size(width = 24.dp, height = 18.dp))
        }) { TrailText(t, IslandType.number, IslandPalette.amberText) }
    }
    is MomentItem -> {
        val t = item.moment.compactValue
        Compact(26.dp, widthOf(t, IslandType.number), item.words, {
            ZenoGlyph(item.moment.feeling, Modifier.size(width = 26.dp, height = 19.dp))
        }) { TrailText(t, IslandType.number, IslandPalette.accentText(item.moment.accent)) }
    }
    is NotePeek -> Compact(22.dp, widthOf(item.title, IslandType.word), "${item.appLabel}: ${item.title}", {
        Avatar(item, 22.dp)
    }) { TrailText(item.title, IslandType.word, IslandPalette.text) }
    is ChargingPeek -> {
        val t = "${item.level}%"
        Compact(16.dp, widthOf(t, IslandType.number), "Charging, $t", { BoltGlyph(Modifier.size(16.dp)) }) {
            TrailText(t, IslandType.number, IslandPalette.zen)
        }
    }
    is QuietDonePeek -> {
        val t = IslandTime.minutes(item.minutes.toLong())
        Compact(24.dp, widthOf(t, IslandType.number), QUIET_DONE, {
            ZenoGlyph(MomentFeeling.PROUD, Modifier.size(width = 24.dp, height = 18.dp))
        }) { TrailText(t, IslandType.number, IslandPalette.zen) }
    }
    is PromisePeek -> {
        val t = "${item.hours}h"
        Compact(24.dp, widthOf(t, IslandType.number), promiseLine(item.hours), {
            ZenoGlyph(MomentFeeling.CALM, Modifier.size(width = 24.dp, height = 18.dp))
        }) { TrailText(t, IslandType.number, IslandPalette.text) }
    }
    else -> null
}

/** The bubble beside the pill: one glyph that says what else is live. */
@Composable
internal fun BubbleFace(item: Any, now: Long, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        when (item) {
            is QuietItem -> {
                Ring(((now - item.startedAt).toFloat() / item.totalMillis), IslandPalette.zen, Modifier.size(size * 0.62f))
                MoonGlyph(Modifier.size(size * 0.42f))
            }
            is CallItem -> PhoneGlyph(Modifier.size(size * 0.46f))
            is LiveItem -> Picture(item.icon, size * 0.56f, size * 0.16f)
            is MediaItem -> Picture(item.art, size * 0.62f, size * 0.31f) { MusicBars(false, Color(item.tint), true, Modifier.size(size * 0.4f)) }
            is LastHourItem -> ZenoGlyph(MomentFeeling.ALERT, Modifier.size(size * 0.66f))
            is MomentItem -> ZenoGlyph(item.moment.feeling, Modifier.size(size * 0.66f))
        }
    }
}

private fun liveValue(item: LiveItem, now: Long): String = when {
    item.chronoBase != null -> IslandTime.clock(if (item.countDown) item.chronoBase - now else now - item.chronoBase)
    item.progress != null -> "${(item.progress * 100).toInt().coerceIn(0, 100)}%"
    else -> item.title
}

@Composable
private fun Avatar(item: NotePeek, size: Dp) {
    when {
        item.own -> ZenoGlyph(MomentFeeling.HAPPY, Modifier.size(size))
        else -> Picture(item.avatar ?: item.icon, size, size / 2) { Monogram(item.title, size) }
    }
}

/** No picture to show: the first letter, on the island's raised tone. */
@Composable
private fun Monogram(name: String, size: Dp) {
    Box(Modifier.size(size).background(IslandPalette.raised), contentAlignment = Alignment.Center) {
        Text(
            name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "·",
            style = IslandType.title.copy(fontSize = (size.value * 0.42f).sp, lineHeight = (size.value * 0.5f).sp),
            color = IslandPalette.text2
        )
    }
}

// ── The open card ───────────────────────────────────────────────────

/** Everything a card can do; [IslandRoot] decides what each does. */
interface CardActions {
    fun open(item: Any)
    fun send(action: IslandAction)
    fun endQuiet()
    fun pet()
    fun mediaToggle()
    fun mediaNext()
    fun mediaPrevious()
    fun touched()
}

/** The card's height for each thing, above the camera band. */
internal fun cardBodyHeight(item: Any): Dp = when (item) {
    is MomentItem -> 136.dp
    is QuietItem -> 156.dp
    is MediaItem -> 158.dp
    is NotePeek -> if (item.actions.isEmpty() && item.open == null) 84.dp else 132.dp
    is CallItem -> 104.dp
    is LiveItem -> if (item.progress != null) 146.dp else 128.dp
    is LastHourItem -> 166.dp
    is QuietDonePeek, is PromisePeek -> 90.dp
    else -> 64.dp
}

@Composable
internal fun Card(item: Any, band: Dp, now: Long, lite: Boolean, act: CardActions, avoidStart: Dp = 0.dp, avoidEnd: Dp = 0.dp) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
        CompositionLocalProvider(LocalBandAvoid provides (avoidStart to avoidEnd)) { CardBody(item, band, now, lite, act) }
    }
}

/** A corner camera's room at the start and end of the band (see [IslandGeometry.avoidStart]). */
private val LocalBandAvoid = staticCompositionLocalOf { 0.dp to 0.dp }

@Composable
private fun CardBody(item: Any, band: Dp, now: Long, lite: Boolean, act: CardActions) {
    run {
        when (item) {
            is MomentItem -> MomentCard(item, band, lite, act)
            is QuietItem -> QuietCard(item, band, now, lite, act)
            is MediaItem -> MediaCard(item, band, lite, act)
            is NotePeek -> NoteCard(item, band, act)
            is CallItem -> CallCard(item, band, now, act)
            is LiveItem -> LiveCard(item, band, now, act)
            is LastHourItem -> LastHourCard(item, band, lite, act)
            is QuietDonePeek -> SayCard(band, QUIET_DONE, IslandTime.minutes(item.minutes.toLong()), IslandPalette.zen, MomentFeeling.PROUD,
                "${item.minutes} minute${if (item.minutes == 1) "" else "s"}, back to you.", if (item.today > 1) "${item.today} sessions today." else "Your first today.", lite)
            is PromisePeek -> SayCard(band, "Your promise", "${item.hours}h", IslandPalette.dusk, MomentFeeling.CALM,
                promiseLine(item.hours), "The rest of the day is yours.", lite)
        }
    }
}

private const val QUIET_DONE = "Quiet done"
private fun promiseLine(hours: Int) = "That's your ${hours}h today."

/** The top band: [left] and [right] beside the camera, nothing under it. */
@Composable
private fun Band(height: Dp, left: @Composable () -> Unit, right: @Composable () -> Unit) {
    val (avoidStart, avoidEnd) = LocalBandAvoid.current
    Row(
        Modifier.fillMaxWidth().height(height).padding(start = (avoidStart - 20.dp).coerceAtLeast(0.dp), end = (avoidEnd - 20.dp).coerceAtLeast(0.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { left() }
        Spacer(Modifier.width(64.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { right() }
    }
}

@Composable
private fun HeroZeno(feeling: MomentFeeling, hue: Color, strength: Float, size: Dp, skin: ZenoSkin? = null, onTap: (() -> Unit)? = null) {
    val tap = if (onTap != null) Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClick = onTap) else Modifier
    Box(Modifier.size(size).glow(hue, strength).then(tap), contentAlignment = Alignment.Center) {
        if (skin != null) ZenoGlyph(feeling, Modifier.size(size * 0.9f), skin) else ZenoGlyph(feeling, Modifier.size(size * 0.9f))
    }
}

@Composable
private fun MomentCard(item: MomentItem, band: Dp, lite: Boolean, act: CardActions) {
    val m = item.moment
    val hue = IslandPalette.hue(m.accent)
    val celebration = m.kind.isCelebration
    Band(band, { Eyebrow(item.eyebrow, IslandPalette.accentText(m.accent)) }) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Flame(16.dp, lit = m.streakWeeks > 0 || celebration)
            Text(if (m.streakWeeks > 0) "${m.streakWeeks}" else "New", style = IslandType.number, color = IslandPalette.text)
            Eyebrow("wk", IslandPalette.mono, Modifier.padding(bottom = 2.dp))
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        HeroZeno(item.moment.feeling, hue, if (lite) 0f else if (celebration) 0.5f else 0.24f, 62.dp, onTap = act::pet)
        Spacer(Modifier.width(14.dp))
        Text(item.words, style = IslandType.headline, color = IslandPalette.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        WeekCells(m.marks, 10.dp, 5.dp)
        item.scoreTenths?.let {
            Spacer(Modifier.width(10.dp))
            Text(MomentCopy.formatTenths(it), style = IslandType.number, color = IslandPalette.text)
            Spacer(Modifier.width(3.dp))
            Eyebrow("Zen", IslandPalette.mono)
        }
        Spacer(Modifier.weight(1f))
        IslandPill("Pet Zeno", true, act::pet, fill = if (celebration) IslandPalette.amber else IslandPalette.zenFill,
            on = if (celebration) IslandPalette.island else IslandPalette.zenOn)
        Spacer(Modifier.width(8.dp))
        IslandPill("Open", false, { act.open(item) })
    }
}

@Composable
private fun QuietCard(item: QuietItem, band: Dp, now: Long, lite: Boolean, act: CardActions) {
    val left = item.endsAt - now
    Band(band, { Eyebrow("Quiet", IslandPalette.zen) }) {
        Eyebrow("Ends ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.endsAt))}", IslandPalette.mono)
    }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(IslandTime.clock(left), style = IslandType.big, color = IslandPalette.text, maxLines = 1)
            Text("Reels and Shorts are quiet.", style = IslandType.body, color = IslandPalette.muted, maxLines = 1)
        }
        Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
            HeroZeno(MomentFeeling.SLEEPY, IslandPalette.moon, if (lite) 0f else 0.16f, 64.dp)
            MoonGlyph(Modifier.size(22.dp).align(Alignment.TopEnd))
        }
    }
    Spacer(Modifier.height(12.dp))
    ProgressLine((now - item.startedAt).toFloat() / item.totalMillis, IslandPalette.zen)
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val started = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.startedAt))
        val note = if (item.held > 0) "${item.held} notification${if (item.held == 1) "" else "s"} waiting" else "Started $started"
        Text(note, style = IslandType.body.copy(fontSize = 12.sp), color = IslandPalette.mono, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        IslandPill("End", false, act::endQuiet)
    }
}

@Composable
private fun MediaCard(item: MediaItem, band: Dp, lite: Boolean, act: CardActions) {
    val tint = Color(item.tint)
    Band(band, { Eyebrow(item.appLabel, IslandPalette.mono) }) {
        MusicBars(item.playing, tint, lite, Modifier.size(width = 22.dp, height = 15.dp))
    }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { act.open(item) }) {
        Picture(item.art, 56.dp, 14.dp) { MusicBars(false, tint, true, Modifier.size(24.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = IslandType.title, color = IslandPalette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.artist, style = IslandType.body, color = IslandPalette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(12.dp))
    val pos = item.positionNow(SystemClock.elapsedRealtime())
    if (item.durationMs > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(IslandTime.clock(pos), style = IslandType.eyebrow, color = IslandPalette.mono)
            ProgressLine(pos.toFloat() / item.durationMs, tint, Modifier.weight(1f).padding(horizontal = 10.dp))
            Text("-" + IslandTime.clock(item.durationMs - pos), style = IslandType.eyebrow, color = IslandPalette.mono)
        }
    } else {
        Spacer(Modifier.height(14.dp))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Control(Icons.Rounded.SkipPrevious, "Previous", 30.dp, act::mediaPrevious)
        Spacer(Modifier.width(30.dp))
        Control(if (item.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (item.playing) "Pause" else "Play", 42.dp, act::mediaToggle)
        Spacer(Modifier.width(30.dp))
        Control(Icons.Rounded.SkipNext, "Next", 30.dp, act::mediaNext)
    }
}

@Composable
private fun Control(icon: ImageVector, label: String, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = IslandPalette.text, modifier = Modifier.size(size))
    }
}

@Composable
private fun NoteCard(item: NotePeek, band: Dp, act: CardActions) {
    Band(band, {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.own) ZenoGlyph(MomentFeeling.HAPPY, Modifier.size(16.dp)) else Picture(item.icon, 16.dp, 4.dp)
            Spacer(Modifier.width(6.dp))
            Eyebrow(item.appLabel, IslandPalette.mono)
        }
    }) { Eyebrow(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.at)), IslandPalette.mono) }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { act.open(item) }) {
        Avatar(item, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = IslandType.title, color = IslandPalette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.text, style = IslandType.body, color = IslandPalette.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    if (item.actions.isNotEmpty() || item.open != null) {
        Spacer(Modifier.height(12.dp))
        Actions(item.actions, act) { act.open(item) }
    }
}

@Composable
private fun Actions(actions: List<IslandAction>, act: CardActions, open: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        actions.forEach { a -> IslandPill(a.label, false, { act.send(a) }) }
        if (open != null) IslandPill("Open", true, open)
    }
}

@Composable
private fun CallCard(item: CallItem, band: Dp, now: Long, act: CardActions) {
    Band(band, {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhoneGlyph(Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Eyebrow(if (item.incoming) "Incoming call" else "On a call", IslandPalette.zen)
        }
    }) { if (!item.incoming) Text(IslandTime.clock(now - item.since), style = IslandType.number, color = IslandPalette.zen) }
    Spacer(Modifier.height(8.dp))
    Text(item.name, style = IslandType.headline.copy(fontSize = 22.sp), color = IslandPalette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(14.dp))
    // As a phone draws them: the first button declines or hangs up (ember), answering is green.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        item.actions.forEachIndexed { i, a ->
            val answer = item.incoming && i == item.actions.lastIndex && i > 0
            when {
                answer -> IslandPill(a.label, true, { act.send(a) })
                i == 0 -> IslandPill(a.label, true, { act.send(a) }, fill = IslandPalette.ember, on = IslandPalette.island)
                else -> IslandPill(a.label, false, { act.send(a) })
            }
        }
        if (!item.incoming) IslandPill("Open", false, { act.open(item) })
    }
}

@Composable
private fun LiveCard(item: LiveItem, band: Dp, now: Long, act: CardActions) {
    Band(band, {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Picture(item.icon, 16.dp, 4.dp) { Monogram(item.appLabel, 16.dp) }
            Spacer(Modifier.width(6.dp))
            Eyebrow(item.appLabel, IslandPalette.mono)
        }
    }) {
        if (item.chronoBase != null || item.progress != null) Text(liveValue(item, now), style = IslandType.number, color = IslandPalette.text)
    }
    Spacer(Modifier.height(6.dp))
    Column(Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { act.open(item) }) {
        Text(item.title, style = IslandType.title, color = IslandPalette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.text, style = IslandType.body, color = IslandPalette.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    item.progress?.let {
        Spacer(Modifier.height(10.dp))
        ProgressLine(it, IslandPalette.zen)
    }
    Spacer(Modifier.height(12.dp))
    Actions(item.actions, act) { act.open(item) }
}

@Composable
private fun LastHourCard(item: LastHourItem, band: Dp, lite: Boolean, act: CardActions) {
    Band(band, { Eyebrow("Last hour", IslandPalette.amberText) }) {
        Text(IslandTime.minutes(item.minutesLeft), style = IslandType.number, color = IslandPalette.amberText)
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        HeroZeno(MomentFeeling.ALERT, IslandPalette.amber, if (lite) 0f else 0.22f, 58.dp)
        Spacer(Modifier.width(14.dp))
        Text(MomentCopy.lastHour(item.minutesLeft), style = IslandType.headline, color = IslandPalette.text, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(14.dp))
    ProgressLine(item.minutesToday.toFloat() / item.promiseMinutes.coerceAtLeast(1L), IslandPalette.amber)
    Spacer(Modifier.height(6.dp))
    Spread {
        Eyebrow("${IslandTime.minutes(item.minutesToday)} today", IslandPalette.mono)
        Eyebrow("of ${IslandTime.minutes(item.promiseMinutes)}", IslandPalette.mono)
    }
    Spacer(Modifier.height(10.dp))
    Actions(emptyList(), act) { act.open(item) }
}

@Composable
private fun SayCard(band: Dp, eyebrow: String, value: String, hue: Color, feeling: MomentFeeling, headline: String, sub: String, lite: Boolean) {
    Band(band, { Eyebrow(eyebrow, hue) }) { Text(value, style = IslandType.number, color = IslandPalette.text) }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        HeroZeno(feeling, hue, if (lite) 0f else 0.26f, 58.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(headline, style = IslandType.headline, color = IslandPalette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, style = IslandType.body, color = IslandPalette.muted, maxLines = 1)
        }
    }
}
