package com.zenlauncher.zenmode.coreapi.services

import com.zenlauncher.zenmode.coreapi.Circle
import com.zenlauncher.zenmode.coreapi.CircleJoinResult
import com.zenlauncher.zenmode.coreapi.ReactionType
import com.zenlauncher.zenmode.coreapi.User
import com.zenlauncher.zenmode.coreapi.UserStats

/**
 * Abstraction over the Firestore-backed data operations.
 * Mirrors the public API of the old FirestoreRepository.
 */
interface FirestoreDataSource {
    suspend fun getBuddyUid(myUid: String): String?
    suspend fun getBuddyStats(buddyUid: String): UserStats?
    suspend fun getUser(uid: String): User?
    suspend fun checkRelationshipExists(myUid: String, otherUid: String): Boolean
    suspend fun sendBuddyInvite(myUid: String, targetUid: String)
    suspend fun disconnectBuddy(myUid: String, buddyUid: String)
    /** Finds a random user with no buddy and creates the relationship atomically.
     *  Returns the matched buddy's UID, or null if no one is available. */
    suspend fun findRandomBuddy(myUid: String): String?
    suspend fun initializeUser(uid: String, displayName: String?)
    suspend fun deleteUser(uid: String)
    /**
     * Returns the epoch-millisecond timestamp of when the relationship between
     * [myUid] and their buddy was created, or null if not found / on error.
     */
    suspend fun getRelationshipCreatedAt(myUid: String): Long?

    /** Canonical relationship document ID for two users (lexicographically ordered). */
    fun getRelationshipId(user1: String, user2: String): String

    /** Atomically increment the sender's like count for today. Returns true on success. */
    suspend fun sendLike(relationshipId: String, senderUid: String): Boolean

    /**
     * Fetch today's like counts for both users in the relationship.
     * Returns Pair(mySent, buddySent). Returns Pair(0, 0) if no document exists or on error.
     */
    suspend fun getTodayLikes(relationshipId: String, myUid: String, buddyUid: String): Pair<Long, Long>

    /** Persist the FCM device token for [uid] so Cloud Functions can target notifications. */
    suspend fun saveFcmToken(uid: String, token: String)

    // ── Zen Circle ────────────────────────────────────────────────────

    /** Reads users/{uid}.circle_id — mirrors [getBuddyUid]'s "find my connection" shape. */
    suspend fun getMyCircleId(myUid: String): String?

    /** Single-doc read — the whole point of the Approach 2 schema (see the plan doc). */
    suspend fun getCircle(circleId: String): Circle?

    /** Creates a new circle with [leaderUid] seeded as its sole initial member. */
    suspend fun createCircle(leaderUid: String, leaderDisplayName: String?, circleName: String): Circle?

    /**
     * Joins [circleId] as [myUid]. Soft cap check (plain read-then-write, not a transaction —
     * occasional 1-2 member overshoot on a race is acceptable and leader-correctable).
     * Mutual-exclusivity (Buddy XOR Circle) IS transaction-guarded: fails with [CircleJoinResult.AlreadyInCircle]
     * unless [confirmedSwitchFromBuddy] is set, mirroring findRandomBuddy's has_buddy transaction guard.
     */
    suspend fun joinCircle(circleId: String, myUid: String, myDisplayName: String?, confirmedSwitchFromBuddy: Boolean = false): CircleJoinResult

    /** Self-leave. Triggers succession server-side if [myUid] was the leader. */
    suspend fun leaveCircle(circleId: String, myUid: String): Boolean

    /** Leader-only removal of another member. */
    suspend fun removeCircleMember(circleId: String, leaderUid: String, targetUid: String): Boolean

    /** Voluntary handoff while the leader is still present — distinct from automatic succession-on-leave. */
    suspend fun transferLeadership(circleId: String, currentLeaderUid: String, newLeaderUid: String): Boolean

    /** Atomic increment on reactions_<from>_<to>.<type>_sent. Rate limiting is enforced client-side, not here. */
    suspend fun sendCircleReaction(circleId: String, fromUid: String, toUid: String, type: ReactionType): Boolean

    /**
     * Today's reactions received BY [myUid], from anyone in the circle -- Pair(loveReceived,
     * meltReceived). Only ever reads your own reactions_<myUid> entry (see
     * fieldReactionsFor), never scans other members' data -- there's no "what you sent them"
     * UI anymore, so there's nothing else to fetch. Pair(0, 0) if the daily_stats doc doesn't
     * exist or on error.
     */
    suspend fun getTodayCircleReactions(circleId: String, myUid: String): Pair<Long, Long>

    /** Finds a random circle with an open slot and joins [myUid] to it atomically, mirroring [findRandomBuddy].
     *  Returns the joined circle, or null if none is available. */
    suspend fun findRandomCircleUser(myUid: String, myDisplayName: String?): Circle?
}

