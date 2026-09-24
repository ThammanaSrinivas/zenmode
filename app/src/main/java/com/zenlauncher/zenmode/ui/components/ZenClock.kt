package com.zenlauncher.zenmode.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

/**
 * The wall clock UI reads "now" from — the reset countdown, the chart's weekday labels, streak
 * dates. Always the system clock in the app; screenshot tests provide a fixed one so goldens
 * don't change with the day or hour they're run at. Read `now` through this, never
 * `LocalDate.now()` / `Calendar.getInstance()` directly, in anything a screenshot test renders.
 */
val LocalZenClock = staticCompositionLocalOf<Clock> { Clock.systemDefaultZone() }
