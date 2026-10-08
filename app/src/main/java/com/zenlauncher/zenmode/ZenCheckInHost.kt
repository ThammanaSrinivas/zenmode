package com.zenlauncher.zenmode

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.recap.RecapStore
import com.zenlauncher.zenmode.ui.components.rememberResumeCount
import java.time.LocalDateTime

/**
 * Home's promise state: the streak on the flame, this week's bars, and today's check-in card
 * if one is due. Lifted out of MainActivity so the decision lives next to [ZenCheckIn] and
 * [PromiseStreak] rather than inside a 1000-line Activity.
 */
internal class ZenCheckInState(
    val streak: PromiseStreak.State,
    /** This week's days, from the same computation the Zen Gold card draws. */
    val weekUnits: List<PromiseUnit>,
    val card: ZenCheckInCard?,
    val dismiss: () -> Unit
)

/**
 * Reads the on-device history on every visit to Home — so a day recorded while the app was
 * backgrounded, or a promise edited in My Promise, shows without a restart — and decides
 * whether a check-in card is due.
 *
 * @param todayMinutes today's live screen time in minutes.
 * @param usageLoaded false until real usage has arrived; a null reading of 0 minutes would
 *   otherwise look like a flawless day and fire a celebration on launch.
 * @param suppressed true while something else owns the screen (the first-run guide, the
 *   entering-ZenMode stage) and no card should be raised over it.
 */
@Composable
internal fun rememberZenCheckIn(
    context: Context,
    recapStore: RecapStore,
    todayMinutes: Long,
    usageLoaded: Boolean,
    suppressed: Boolean
): ZenCheckInState {
    val resumeCount = rememberResumeCount()
    val promiseHours = remember(resumeCount) { PromisePreferences.getDailyHours(context) }
    val todayKept = todayMinutes <= promiseHours * 60L

    // The streak Home shows is the promise streak: a day over the line holds it, only a lost
    // week resets it (see PromiseStreak). AppLogic.getStreakCount's stricter mindful-day run
    // stays where it's labelled as such — the milestone card's "longest".
    val streak = remember(resumeCount, todayMinutes, promiseHours) {
        PromiseStreak.of(recapStore.days(), todayKept)
    }
    val weekUnits = remember(resumeCount, todayMinutes, promiseHours) {
        ZenGoldPromise.weekly(recapStore.days(), todayMinutes, promiseHours).units
    }

    var card by remember { mutableStateOf<ZenCheckInCard?>(null) }

    LaunchedEffect(resumeCount, todayMinutes, usageLoaded, suppressed) {
        if (!usageLoaded || suppressed) return@LaunchedEffect
        if (card != null) return@LaunchedEffect
        if (!ZenCheckInPreferences.isEnabled(context)) return@LaunchedEffect

        val due = ZenCheckIn.due(
            now = LocalDateTime.now(),
            eveningAt = ZenCheckInPreferences.getEveningTime(context),
            promiseHours = promiseHours,
            todayMinutes = todayMinutes,
            streak = streak,
            lastShown = { slot -> ZenCheckInPreferences.getLastShown(context, slot) }
        ) ?: return@LaunchedEffect

        // Marked shown as it's raised, not when dismissed: a card the user walks away from by
        // leaving Home has still had its turn today.
        ZenCheckInPreferences.setLastShown(context, due.kind.slot)
        ServiceLocator.analyticsTracker.trackCheckInShown(
            due.kind.key,
            streak.days,
            streak.daysKeptThisWeek
        )
        card = due
    }

    return ZenCheckInState(
        streak = streak,
        weekUnits = weekUnits,
        card = card,
        dismiss = { card = null }
    )
}
