package com.zenlauncher.zenmode.share

import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.recap.RecapOutcome
import com.zenlauncher.zenmode.recap.RecapStory
import com.zenlauncher.zenmode.recap.WeeklyRecap
import com.zenlauncher.zenmode.recap.formatMinutes
import com.zenlauncher.zenmode.recap.fullDayName
import com.zenlauncher.zenmode.recap.rangeLabel
import java.util.Locale

/** How a week's story is told: all seven kept, the promise kept, or an honest miss. */
enum class WeekTier { PERFECT, KEPT, MISSED }

/**
 * Everything the weekly story says, from one finished [WeeklyRecap] — the same numbers the
 * recap and the PDF report use. App names never appear: the story is shared outside the app.
 */
data class WeeklyShare(val recap: WeeklyRecap) {
    val tier: WeekTier = when {
        recap.days.size == 7 && recap.daysKept == 7 -> WeekTier.PERFECT
        recap.outcome == RecapOutcome.KEPT -> WeekTier.KEPT
        else -> WeekTier.MISSED
    }

    val eyebrow: String = "MY WEEK IN ZEN · ${recap.rangeLabel(Locale.US).uppercase(Locale.US)}"

    val headline: String = when (tier) {
        WeekTier.PERFECT -> "A perfect week."
        WeekTier.KEPT -> "Promise kept."
        WeekTier.MISSED -> "An honest week."
    }

    val total: String = formatMinutes(recap.totalMinutes)
    val kept: String = "${recap.daysKept}/${recap.days.size}"
    val keptLabel: String = "DAYS UNDER ${recap.promiseHours}H"

    /** Minutes against the week before: negative is better. Null when there's no week before. */
    val change: Long? = recap.previousWeekTotalMinutes?.let { recap.totalMinutes - it }
    val changeLabel: String? = change?.let {
        when {
            it < 0 -> "▼ ${formatMinutes(-it)} vs last week"
            it > 0 -> "▲ ${formatMinutes(it)} vs last week"
            else -> "Level with last week"
        }
    }
    val changeBetter: Boolean = (change ?: 0L) <= 0L

    /** Two lines under the chart: the best of the week, then what it adds up to — or, for a
     * missed week, the hardest day and one thing to try. */
    val highlights: List<PosterStat> = when (tier) {
        WeekTier.PERFECT, WeekTier.KEPT -> listOfNotNull(
            recap.calmestDay?.let { PosterStat("CALMEST DAY", "${it.fullDayName(Locale.US)} · ${formatMinutes(it.screenTimeMinutes)}") },
            recap.minutesReclaimed?.takeIf { it >= 30 }?.let {
                PosterStat("WON BACK", "${formatMinutes(it)}, ${RecapStory.equivalentOf(it)}")
            } ?: PosterStat("DAILY AVERAGE", "${formatMinutes(recap.dailyAverageMinutes)} a day")
        )
        WeekTier.MISSED -> listOfNotNull(
            recap.loudestDay?.let { PosterStat("TOUGHEST DAY", "${it.fullDayName(Locale.US)} · ${formatMinutes(it.screenTimeMinutes)}") },
            PosterStat(
                "NEXT WEEK",
                RecapStory.suggestedPromiseHours(recap)?.takeIf { it != recap.promiseHours }
                    ?.let { "Trying a $it-hour promise" }
                    ?: "One screen-free hour a day"
            )
        )
    }

    val shareText: String = when (tier) {
        WeekTier.PERFECT -> "Seven for seven: a perfect week of calmer screen time with ZenMode OS. " +
            AppConstants.PLAY_STORE_URL
        WeekTier.KEPT -> "Kept my screen-time promise ${recap.daysKept} of 7 days this week with ZenMode OS. " +
            AppConstants.PLAY_STORE_URL
        WeekTier.MISSED -> "Not every week is calm — ${recap.daysKept} of 7 this time, and a fresh one starts now. " +
            "ZenMode OS: ${AppConstants.PLAY_STORE_URL}"
    }

    val description: String = "$headline ${recap.rangeLabel(Locale.US)}: $total on the phone, " +
        "${recap.daysKept} of ${recap.days.size} days under a ${recap.promiseHours}-hour promise. " +
        highlights.joinToString(". ") { "${it.label.lowercase(Locale.US)}: ${it.value}" }

    /** What analytics calls this card. */
    val analyticsKey: String = "week_${tier.name.lowercase(Locale.US)}"

    /** File names for the image and the clip. */
    val fileBaseName: String = "zenmode_week_${recap.weekStart}"
}
