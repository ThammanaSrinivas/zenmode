package com.zenlauncher.zenmode.coreapi

/**
 * Lives here (not app's AppConstants) because core-private's FirestoreDataSourceImpl needs it
 * for the join cap check, and core-private cannot depend on the app module. Also duplicated,
 * unavoidably, as a literal in zenmode_core_private/firestore.rules (rules can't import Kotlin
 * constants) and in ZenCircleSettingsSheet.kt's ZenCircleCapacity, which should reference this
 * constant rather than redefine it. Keep all three in sync if this ever changes.
 */
const val ZEN_CIRCLE_MAX_MEMBERS = 7

enum class CircleRole { LEADER, MEMBER }

enum class ReactionType { LOVE, MELT }

data class CircleMember(
    val uid: String,
    val displayName: String?,
    val zenScore: Int,               // 0-100, see AppLogic.calculateZenScore
    val screenTimeMinutes: Long = 0L, // mirrored server-side alongside zenScore, same trigger
    val lastUpdatedEpochMs: Long,    // score-sync timestamp; doubles as the activity signal for succession
    val role: CircleRole,
    val joinedAtEpochMs: Long
)

data class Circle(
    val id: String,                  // also doubles as the shareable invite code
    val name: String,
    val leaderUid: String,
    val members: List<CircleMember>, // domain shape hides the Firestore map-vs-array storage detail
    val createdAtEpochMs: Long
)

sealed class CircleJoinResult {
    object Success : CircleJoinResult()
    object CircleFull : CircleJoinResult()
    object AlreadyInCircle : CircleJoinResult()          // caller must resolve the Buddy/Circle switch first
}
