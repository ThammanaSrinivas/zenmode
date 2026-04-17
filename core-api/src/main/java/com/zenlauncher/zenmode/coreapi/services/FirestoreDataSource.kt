package com.zenlauncher.zenmode.coreapi.services

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
}

