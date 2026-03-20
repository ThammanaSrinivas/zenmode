package com.zenlauncher.zenmode.coreapi.services

import com.zenlauncher.zenmode.coreapi.SignInResult

/**
 * Provides access to authentication without exposing Firebase Auth.
 */
interface AuthProvider {
    fun getCurrentUserId(): String?
    fun isSignedIn(): Boolean

    /**
     * Signs in with a Google ID token. Returns a [SignInResult] with user info.
     * The implementation handles Firebase Auth internally.
     */
    suspend fun signInWithGoogleToken(idToken: String): SignInResult
}

