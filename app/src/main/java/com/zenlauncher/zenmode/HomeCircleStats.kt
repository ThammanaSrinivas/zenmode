package com.zenlauncher.zenmode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zenlauncher.zenmode.coreapi.Circle

/** One other circle member, reduced to what Home's stats card needs. */
data class HomeStackMember(
    val uid: String,
    val name: String,
    val stats: BuddyStats,
    val zenScore: Int,
    val lastUpdatedEpochMs: Long
)

/**
 * Other circle members (everyone but [myUid]), most-recently-active first. Home's single-card
 * slot shows stackMembers.firstOrNull(); a multi-member circle queues the rest behind it.
 * "Most-recently-active" (not join order or rank) so the featured card is whoever's actually
 * around today.
 */
fun deriveHomeStackMembers(circle: Circle?, myUid: String?): List<HomeStackMember> =
    circle?.members
        ?.filter { it.uid != myUid && it.uid.isNotBlank() }
        ?.sortedByDescending { it.lastUpdatedEpochMs }
        ?.map {
            HomeStackMember(
                uid = it.uid,
                name = it.displayName?.takeIf { n -> n.isNotBlank() } ?: "Member",
                stats = BuddyStats(screenTimeMins = it.screenTimeMinutes),
                zenScore = it.zenScore,
                lastUpdatedEpochMs = it.lastUpdatedEpochMs
            )
        }
        ?: emptyList()

/** What Home's right-hand stats card should show, folding Buddy + Zen Circle into one shape. */
data class HomeBuddyCardState(
    val show: Boolean,
    val stats: BuddyStats?,
    val zenScoreOverride: Int?,
    val streaksOverride: Int?,
    /** 2+ other circle members to queue behind the front card, see HomeBuddyCardStack -- always
     * empty when [show] comes from the classic Buddy relationship, not a real Circle. */
    val stackMembers: List<HomeStackMember>
)

/**
 * Home is otherwise circle-blind -- MainViewModel's hasBuddies/buddyStats only ever look at the
 * classic Buddy relationship, so a circle-only user would see the empty "Get your Zen Bro!"
 * invite card instead of a real circle-mate's stats. Classic Buddy takes priority when both
 * exist (mutual exclusivity is already enforced server-side, but stay defensive here too).
 */
@Composable
fun rememberHomeBuddyCardState(
    circle: Circle?,
    myUid: String?,
    hasBuddies: Boolean,
    buddyStats: BuddyStats?
): HomeBuddyCardState {
    val stackMembers = remember(circle, myUid) { deriveHomeStackMembers(circle, myUid) }
    val featured = stackMembers.firstOrNull()
    return remember(hasBuddies, buddyStats, stackMembers) {
        HomeBuddyCardState(
            show = hasBuddies || featured != null,
            stats = buddyStats ?: featured?.stats,
            zenScoreOverride = featured?.zenScore.takeIf { !hasBuddies },
            // Circles don't track per-member streaks yet -- 0 is the same convention
            // CircleStageMembers.kt already uses for a real circle-mate's streak.
            streaksOverride = 0.takeIf { !hasBuddies && featured != null },
            stackMembers = if (hasBuddies) emptyList() else stackMembers
        )
    }
}
