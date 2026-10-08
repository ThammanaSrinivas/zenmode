package com.zenlauncher.zenmode.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.ui.components.LocalZenClock
import com.zenlauncher.zenmode.ui.components.MoodSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * Every golden renders at this one instant, so clock-driven text (the "ranks reset in" countdown,
 * the chart's weekday labels, streak dates) doesn't drift with the day or hour the suite runs.
 * A Sunday afternoon: the settings chart reads MON..SUN and the circle resets in 10:35.
 */
private val GoldenClock: Clock = Clock.fixed(Instant.parse("2026-09-13T13:25:00Z"), ZoneOffset.UTC)

/**
 * [Paparazzi.snapshot] with everything that would otherwise come from the machine running it
 * pinned, so a golden recorded on one laptop matches on CI and on every other laptop:
 *
 *  · the clock — [LocalZenClock] at [GoldenClock];
 *  · the locale — month and day names format through the JVM default ("Sep" in the US, "Sept"
 *    in the UK and India), so it's [Locale.US] for the snapshot and restored after;
 *  · the mood — pages paint today's wash from [MoodSource.lastKnown] before usage loads, a
 *    process-wide cache the Home goldens write, so without a reset a page's colours would
 *    depend on which test happened to run before it.
 *
 * Use for every golden.
 */
fun Paparazzi.golden(content: @Composable () -> Unit) {
    val machineLocale = Locale.getDefault()
    Locale.setDefault(Locale.US)
    MoodSource.lastKnown = null
    try {
        snapshot { CompositionLocalProvider(LocalZenClock provides GoldenClock) { content() } }
    } finally {
        Locale.setDefault(machineLocale)
        MoodSource.lastKnown = null
    }
}
