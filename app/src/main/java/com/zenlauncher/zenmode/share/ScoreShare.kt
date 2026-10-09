package com.zenlauncher.zenmode.share

import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.coreapi.ZenScore
import com.zenlauncher.zenmode.recap.formatMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How a Zen Score card reads, by the score itself. The card draws the day as water — still at
 * the top of the scale, churning at the bottom — so the five bands are five states of it.
 */
enum class ScoreBand(val minTenths: Int, val label: String) {
    STORMY(0, "a stormy one"),
    CHOPPY(40, "a choppy one"),
    STEADY(60, "a steady day"),
    CALM(80, "a calm day"),
    STILL(90, "still water");

    companion object {
        fun of(tenths: Int): ScoreBand = entries.last { tenths.coerceIn(0, ZenScore.MAX_TENTHS) >= it.minTenths }
    }
}

/**
 * Everything a Zen Score card says. [reclaimedMinutes] is today against yesterday at this same
 * hour (ZenScoreActivity), so it's labelled as today's, never as a monthly figure.
 */
data class ScoreShare(
    val tenths: Int,
    val yesterdayTenths: Int?,
    val reclaimedMinutes: Int,
    val today: LocalDate
) {
    val band: ScoreBand = ScoreBand.of(tenths)
    val score: String = ZenScore.format(tenths)

    val title: String = when (band) {
        ScoreBand.STILL -> "Still water."
        ScoreBand.CALM -> "Calm water."
        ScoreBand.STEADY -> "Steady."
        ScoreBand.CHOPPY -> "Choppy, still afloat."
        ScoreBand.STORMY -> "A stormy one."
    }

    val caption: String = when (band) {
        ScoreBand.STILL -> "$score out of 10 today. Barely a ripple on the day."
        ScoreBand.CALM -> "$score out of 10 today. A calm day, spent on purpose."
        ScoreBand.STEADY -> "$score out of 10 today. More calm than noise."
        ScoreBand.CHOPPY -> "$score out of 10 today. Some noise, and I'm still steering."
        ScoreBand.STORMY -> "$score out of 10 today. A loud one. Tomorrow the water resets."
    }

    /** "+0.6", "−0.4", "LEVEL", or a dash before there's a yesterday to compare with. */
    val delta: String = when {
        yesterdayTenths == null -> "—"
        tenths > yesterdayTenths -> "+${ZenScore.formatDelta(tenths - yesterdayTenths)}"
        tenths < yesterdayTenths -> "−${ZenScore.formatDelta(tenths - yesterdayTenths)}"
        else -> "LEVEL"
    }

    val stamp: String = "ZEN SCORE"

    /** The line under the card in the sheet. */
    val note: String = when {
        yesterdayTenths == null -> "Tomorrow you'll see how today compares."
        tenths > yesterdayTenths -> "Up ${ZenScore.formatDelta(tenths - yesterdayTenths)} on yesterday."
        tenths < yesterdayTenths -> "Down ${ZenScore.formatDelta(tenths - yesterdayTenths)} on yesterday. There's still time today."
        else -> "Level with yesterday."
    }

    /** What analytics calls this card. */
    val analyticsKey: String = "score_${band.name.lowercase(Locale.US)}"

    val stats: List<PosterStat> = listOf(
        PosterStat("TODAY", "$score/10"),
        PosterStat("VS YESTERDAY", delta),
        PosterStat("WON BACK", formatMinutes(reclaimedMinutes.coerceAtLeast(0).toLong()))
    )

    val footer: String = today.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)).uppercase(Locale.US)

    val shareText: String =
        "$score/10 on $PRODUCT_NAME today — ${band.label}. What's yours? ${AppConstants.PLAY_STORE_URL}"

    val description: String = "$title $caption Change from yesterday $delta. " +
        "${formatMinutes(reclaimedMinutes.coerceAtLeast(0).toLong())} won back today."
}
