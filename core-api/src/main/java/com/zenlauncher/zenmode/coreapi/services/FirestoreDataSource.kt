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
}

