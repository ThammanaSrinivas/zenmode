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
    suspend fun initializeUser(uid: String, displayName: String?)
}

