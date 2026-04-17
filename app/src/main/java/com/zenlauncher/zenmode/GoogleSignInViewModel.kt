package com.zenlauncher.zenmode

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

sealed class SignInUiState {
    object Idle : SignInUiState()
    object Loading : SignInUiState()
    object Success : SignInUiState()
    data class Error(val message: String) : SignInUiState()
    data class LaunchIntent(val intent: Intent) : SignInUiState()
}

class GoogleSignInViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val context = application.applicationContext
    private var credentialJob: Job? = null

    /**
     * Performs the entire getCredential flow inside viewModelScope.
     */
    fun performGetCredential(activity: Activity, redirectIfNoAccount: Boolean = true) {
        _uiState.value = SignInUiState.Loading

        val primaryRequest = buildSignInWithGoogleRequest() ?: return
        val credentialManager = CredentialManager.create(context)
        val activityRef = WeakReference(activity)

        credentialJob?.cancel()
        credentialJob = viewModelScope.launch {
            val activityContext = activityRef.get()
            if (activityContext == null) {
                _uiState.value = SignInUiState.Error("Activity no longer available")
                return@launch
            }
            try {
                val result = credentialManager.getCredential(
                    request = primaryRequest,
                    context = activityContext,
                )
                handleSignInResult(result)
            } catch (primaryError: NoCredentialException) {
                val fallbackRequest = buildGoogleIdRequest()
                if (fallbackRequest == null) {
                    handleSignInError(primaryError, redirectIfNoAccount)
                    return@launch
                }
                try {
                    val result = credentialManager.getCredential(
                        request = fallbackRequest,
                        context = activityContext,
                    )
                    handleSignInResult(result)
                } catch (fallbackError: Exception) {
                    handleSignInError(fallbackError, redirectIfNoAccount)
                }
            } catch (e: Exception) {
                handleSignInError(e, redirectIfNoAccount)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        credentialJob?.cancel()
    }

    fun buildSignInWithGoogleRequest(): GetCredentialRequest? {
        val webClientId = resolveWebClientId() ?: return null
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }

    fun buildGoogleIdRequest(): GetCredentialRequest? {
        val webClientId = resolveWebClientId() ?: return null
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }

    private fun resolveWebClientId(): String? {
        val webClientId = BuildConfig.WEB_CLIENT_ID
        if (webClientId == "YOUR_WEB_CLIENT_ID" || webClientId.isEmpty()) {
            _uiState.value = SignInUiState.Error("Web Client ID is missing. Check local.properties")
            return null
        }
        return webClientId
    }

    fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                _uiState.value = SignInUiState.Error("Invalid Credential Data")
            }
        } else {
            _uiState.value = SignInUiState.Error("Unexpected credential type")
        }
    }

    fun handleSignInError(e: Throwable, redirectIfNoAccount: Boolean = true) {
        when (e) {
            is NoCredentialException -> {
                if (redirectIfNoAccount) {
                    try {
                        val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
                        intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                        _uiState.value = SignInUiState.LaunchIntent(intent)
                    } catch (ex: Exception) {
                        _uiState.value = SignInUiState.Error("Could not launch Add Account settings")
                    }
                } else {
                    _uiState.value = SignInUiState.Error("Sign In Cancelled: No account added.")
                }
            }
            is GetCredentialException -> {
                _uiState.value = SignInUiState.Error("Sign In Failed: ${e.message}")
            }
            else -> {
                _uiState.value = SignInUiState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            val signInResult = ServiceLocator.authProvider.signInWithEmailAndPassword(email, password)
            handleSignInResult(signInResult)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        viewModelScope.launch {
            val signInResult = ServiceLocator.authProvider.signInWithGoogleToken(idToken)
            handleSignInResult(signInResult)
        }
    }

    private suspend fun handleSignInResult(signInResult: com.zenlauncher.zenmode.coreapi.SignInResult) {
        if (signInResult.isSuccess) {
            val analyticsManager = ServiceLocator.analyticsManager
            val repository = UsageRepository(context, analyticsManager)
            val userId = signInResult.userId

            if (userId != null) {
                repository.saveUserUid(userId)
                analyticsManager.identifyUser(userId, mapOf(
                    "name" to (signInResult.displayName ?: ""),
                    "email" to (signInResult.email ?: "")
                ))
                repository.setPostHogIdentified(true)
            }

            if (signInResult.isNewUser && userId != null) {
                initializeUserInFirestore(userId, signInResult.displayName)
            } else {
                _uiState.value = SignInUiState.Success
            }
        } else {
            _uiState.value = SignInUiState.Error(signInResult.errorMessage ?: "Authentication Failed")
        }
    }

    private fun initializeUserInFirestore(uid: String, displayName: String?) {
        viewModelScope.launch {
            try {
                ServiceLocator.firestoreDataSource.initializeUser(uid, displayName)
            } catch (_: Exception) {
                // Proceed even if Firestore write fails (offline mode etc)
            }
            _uiState.value = SignInUiState.Success
        }
    }

    fun resetState() {
        _uiState.value = SignInUiState.Idle
    }
}
