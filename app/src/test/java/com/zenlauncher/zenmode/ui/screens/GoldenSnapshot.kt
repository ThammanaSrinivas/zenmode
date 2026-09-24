package com.zenlauncher.zenmode.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.ui.components.LocalZenClock
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Every golden renders at this one instant, so clock-driven text (the "ranks reset in" countdown,
 * the chart's weekday labels, streak dates) doesn't drift with the day or hour the suite runs.
 * A Sunday afternoon: the settings chart reads MON..SUN and the circle resets in 10:35.
 */
private val GoldenClock: Clock = Clock.fixed(Instant.parse("2026-09-13T13:25:00Z"), ZoneOffset.UTC)

/** [Paparazzi.snapshot] with [LocalZenClock] pinned to [GoldenClock]. Use for every golden. */
fun Paparazzi.golden(content: @Composable () -> Unit) = snapshot {
    CompositionLocalProvider(LocalZenClock provides GoldenClock) { content() }
}
