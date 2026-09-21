package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.coreapi.Circle
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.screens.ZenCircleMember

/**
 * Builds the ZenCircleScreen member list for the Zen Bro "Circle" stage. Three cases:
 * - [realCircle] non-null: its real members, padded with a dummy "invite pending" slot
 *   until a second person actually joins (uid left blank -- see MainActivity's onSendLove/
 *   onSendMelt guard against reacting to it).
 * - No real circle yet, but [isCircleMode]: just "You", waiting on createCircle()/joinCircle()
 *   to land (see MainActivity's Share-link/Use-a-code auto-create wiring).
 * - Otherwise: the classic 2-person Buddy reskin, using [buddyNameFallback].
 */
fun buildCircleStageMembers(
    realCircle: Circle?,
    isCircleMode: Boolean,
    buddyNameFallback: String,
    userCode: String?,
    usage: DailyUsage?,
    zenScore: Int,
    streakCount: Int,
    yesterdayChangePercent: Int?,
    buddyStats: BuddyStats?,
    // Pair(loveReceivedToday, meltReceivedToday) -- what YOU received today, from anyone. Only
    // ever attached to the "you" member below; nobody else's card shows a reaction badge. See
    // FirestoreDataSource.getTodayCircleReactions / CircleUiState.reactions.
    reactions: Pair<Long, Long> = 0L to 0L
): List<ZenCircleMember> {
    if (realCircle != null) {
        val real = realCircle.members.map { m ->
            ZenCircleMember(
                name = if (m.uid == userCode) "You" else (m.displayName?.takeIf { it.isNotBlank() } ?: "Member"),
                isYou = m.uid == userCode,
                // "You" gets the live local value (updates immediately, not just on the next
                // sync); other members get the server-mirrored screenTimeMinutes -- see
                // onUserScoreSynced (functions/src/index.ts) in zenmode_core_private.
                screenTimeMinutes = if (m.uid == userCode) (usage?.screenTimeInMillis ?: 0L) / 60_000 else m.screenTimeMinutes,
                zenScore = m.zenScore,
                // Real circles don't track per-member streaks yet (not in the schema) --
                // only "you" gets the real local value.
                streaks = if (m.uid == userCode) streakCount else 0,
                changePercent = if (m.uid == userCode) yesterdayChangePercent else null,
                uid = m.uid,
                loveReceivedToday = if (m.uid == userCode) reactions.first else 0L,
                meltReceivedToday = if (m.uid == userCode) reactions.second else 0L
            )
        }
        return if (real.size < 2) {
            real + ZenCircleMember(name = "Invite pending", isYou = false, screenTimeMinutes = 0L, zenScore = 0, streaks = 0)
        } else real
    }

    val you = ZenCircleMember(
        name = "You",
        isYou = true,
        screenTimeMinutes = (usage?.screenTimeInMillis ?: 0L) / 60_000,
        zenScore = zenScore,
        streaks = streakCount,
        changePercent = yesterdayChangePercent
    )
    if (isCircleMode) return listOf(you)

    return listOf(
        you,
        ZenCircleMember(
            name = buddyNameFallback,
            isYou = false,
            screenTimeMinutes = buddyStats?.screenTimeMins ?: 0L,
            zenScore = AppConstants.PLACEHOLDER_BUDDY_ZEN_SCORE,
            streaks = AppConstants.PLACEHOLDER_BUDDY_STREAK
        )
    )
}
