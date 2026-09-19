package com.zenlauncher.zenmode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zenlauncher.zenmode.coreapi.Circle
import com.zenlauncher.zenmode.coreapi.CircleJoinResult
import com.zenlauncher.zenmode.coreapi.ReactionType
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import kotlinx.coroutines.launch

data class CircleUiState(
    val circle: Circle? = null,
    val loading: Boolean = false,
    val removing: Boolean = false,
    /** One-shot: true right after a successful leaveCircle(). The host must navigate away
     * (mirroring AccountabilityUiState.disconnectResult's LaunchedEffect pattern in
     * MainActivity) rather than let the screen fall back to rendering a placeholder
     * "buddyName" member that doesn't correspond to a real person. Consume via
     * [CircleViewModel.consumeLeftCircleEvent]. */
    val justLeftCircle: Boolean = false,
    /** Set when a join attempt hit AlreadyInCircle and the caller has a classic buddy --
     * the UI shows the switch-confirmation popup and calls confirmSwitchFromBuddyAndJoin. */
    val pendingBuddySwitchCircleId: String? = null,
    val errorMessage: String? = null
)

class CircleViewModel(private val repository: UsageRepository) : ViewModel() {

    private val firestoreDataSource = ServiceLocator.firestoreDataSource

    private val _uiState = MutableLiveData(CircleUiState())
    val uiState: LiveData<CircleUiState> get() = _uiState

    init {
        // Offline-first: show the cached circle immediately, same pattern as
        // AccountabilityViewModel.refreshBuddyStatsFromCache.
        repository.getCachedCircle()?.let { cached ->
            _uiState.value = _uiState.value!!.copy(circle = cached)
        }
        loadCircle()

        // Live-refresh when a circle_react FCM push arrives while the app is open.
        viewModelScope.launch {
            ServiceLocator.circleReactedEvents.collect {
                loadCircle()
            }
        }
    }

    private fun myUid(): String? =
        repository.getUserUid() ?: ServiceLocator.authProvider.getCurrentUserId()

    fun loadCircle() {
        val myUid = myUid() ?: return
        _uiState.postValue(_uiState.value!!.copy(loading = true))
        viewModelScope.launch {
            var circleId = repository.getCachedCircleId()?.takeIf { it.isNotEmpty() }
            var circle = circleId?.let { firestoreDataSource.getCircle(it) }

            // Cached circleId didn't resolve to a real circle -- either there was never a
            // cache hit, or the cache is stale (circle deleted, or the user was removed /
            // joined a different circle from another device). Re-verify against the server
            // instead of trusting a dead cached ID indefinitely.
            if (circleId == null || circle == null) {
                circleId = firestoreDataSource.getMyCircleId(myUid)
                circle = circleId?.let { firestoreDataSource.getCircle(it) }
            }

            repository.saveCircleId(circleId)
            if (circle != null) {
                repository.cacheCircle(circle)
            } else {
                repository.clearCachedCircle()
            }
            _uiState.postValue(_uiState.value!!.copy(circle = circle, loading = false))
        }
    }

    fun createCircle(circleName: String) {
        val myUid = myUid() ?: return
        val displayName = ServiceLocator.authProvider.getDisplayName()
        _uiState.postValue(_uiState.value!!.copy(loading = true))
        viewModelScope.launch {
            val circle = firestoreDataSource.createCircle(myUid, displayName, circleName)
            if (circle != null) {
                repository.saveCircleId(circle.id)
                repository.cacheCircle(circle)
                ServiceLocator.analyticsTracker.trackCircleCreated()
            }
            _uiState.postValue(
                _uiState.value!!.copy(
                    circle = circle,
                    loading = false,
                    errorMessage = if (circle == null) "Couldn't create circle" else null
                )
            )
        }
    }

    /** [via] is "invite_link" or "code", for analytics. */
    fun joinCircle(circleId: String, via: String) {
        val myUid = myUid() ?: return
        val myHasBuddy = repository.hasCachedBuddy()
        _uiState.postValue(_uiState.value!!.copy(loading = true))
        viewModelScope.launch {
            val displayName = ServiceLocator.authProvider.getDisplayName()
            val result = firestoreDataSource.joinCircle(circleId, myUid, displayName, confirmedSwitchFromBuddy = false)
            when (result) {
                is CircleJoinResult.Success -> {
                    repository.saveCircleId(circleId)
                    ServiceLocator.analyticsTracker.trackCircleJoined(via)
                    loadCircle()
                }
                is CircleJoinResult.CircleFull -> {
                    _uiState.postValue(_uiState.value!!.copy(loading = false, errorMessage = "This circle is full"))
                }
                is CircleJoinResult.AlreadyInCircle -> {
                    if (myHasBuddy) {
                        // Let the UI show the switch-confirmation popup rather than a bare error.
                        _uiState.postValue(_uiState.value!!.copy(loading = false, pendingBuddySwitchCircleId = circleId))
                    } else {
                        _uiState.postValue(_uiState.value!!.copy(loading = false, errorMessage = "You're already in a circle"))
                    }
                }
            }
        }
    }

    /** Called after the user confirms the Buddy->Circle switch popup. */
    fun confirmSwitchFromBuddyAndJoin(circleId: String) {
        val myUid = myUid() ?: return
        val buddyUid = repository.getBuddyUid()
        _uiState.postValue(_uiState.value!!.copy(loading = true, pendingBuddySwitchCircleId = null))
        viewModelScope.launch {
            try {
                if (buddyUid != null) {
                    firestoreDataSource.disconnectBuddy(myUid, buddyUid)
                    repository.clearCachedBuddy()
                }
                val displayName = ServiceLocator.authProvider.getDisplayName()
                val result = firestoreDataSource.joinCircle(circleId, myUid, displayName, confirmedSwitchFromBuddy = true)
                if (result is CircleJoinResult.Success) {
                    repository.saveCircleId(circleId)
                    ServiceLocator.analyticsTracker.trackBuddyToCircleSwitch()
                    ServiceLocator.analyticsTracker.trackCircleJoined("switch_from_buddy")
                    loadCircle()
                } else {
                    // Deliberately NOT the usual silent-safe-default here: disconnect already
                    // succeeded, so silence would leave the user thinking they joined when
                    // they're actually in neither Buddy nor Circle. See plan doc's Risks section.
                    _uiState.postValue(
                        _uiState.value!!.copy(
                            loading = false,
                            errorMessage = "Disconnected your buddy, but couldn't join the circle -- please try the invite link again."
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.postValue(
                    _uiState.value!!.copy(
                        loading = false,
                        errorMessage = "Something went wrong switching to Circle -- please try again."
                    )
                )
            }
        }
    }

    fun dismissBuddySwitchPrompt() {
        _uiState.postValue(_uiState.value!!.copy(pendingBuddySwitchCircleId = null))
    }

    fun leaveCircle() {
        val myUid = myUid() ?: return
        val circleId = _uiState.value?.circle?.id ?: return
        _uiState.postValue(_uiState.value!!.copy(removing = true))
        viewModelScope.launch {
            val ok = firestoreDataSource.leaveCircle(circleId, myUid)
            if (ok) {
                repository.clearCachedCircle()
                ServiceLocator.analyticsTracker.trackCircleLeft()
                _uiState.postValue(CircleUiState(justLeftCircle = true))
            } else {
                _uiState.postValue(_uiState.value!!.copy(removing = false, errorMessage = "Couldn't leave circle"))
            }
        }
    }

    /** Call once the host has navigated away in response to justLeftCircle, same lifecycle
     * as AccountabilityViewModel.resetDisconnectResult(). */
    fun consumeLeftCircleEvent() {
        _uiState.postValue(_uiState.value!!.copy(justLeftCircle = false))
    }

    fun removeMember(targetUid: String) {
        val myUid = myUid() ?: return
        val circleId = _uiState.value?.circle?.id ?: return
        _uiState.postValue(_uiState.value!!.copy(removing = true))
        viewModelScope.launch {
            val ok = firestoreDataSource.removeCircleMember(circleId, myUid, targetUid)
            ServiceLocator.analyticsTracker.trackCircleMemberRemoved(byLeader = true)
            _uiState.postValue(_uiState.value!!.copy(removing = false))
            if (ok) loadCircle()
        }
    }

    fun transferLeadership(newLeaderUid: String) {
        val myUid = myUid() ?: return
        val circleId = _uiState.value?.circle?.id ?: return
        viewModelScope.launch {
            val ok = firestoreDataSource.transferLeadership(circleId, myUid, newLeaderUid)
            if (ok) {
                ServiceLocator.analyticsTracker.trackCircleLeadershipTransferred("voluntary")
                loadCircle()
            }
        }
    }

    /** Client-side rate limit, per target -- see UsageRepository.getRecentReactionTimestamps. */
    fun sendReaction(toUid: String, type: ReactionType) {
        val myUid = myUid() ?: return
        val circleId = _uiState.value?.circle?.id ?: return

        val recent = repository.getRecentReactionTimestamps(toUid)
        if (recent.size >= UsageRepository.CIRCLE_REACTION_MAX_COUNT) {
            val oldest = recent.first()
            val waitMs = (oldest + UsageRepository.CIRCLE_REACTION_WINDOW_MS) - System.currentTimeMillis()
            _uiState.postValue(_uiState.value!!.copy(errorMessage = formatWaitToast(waitMs)))
            return
        }
        repository.recordReactionSent(toUid)

        viewModelScope.launch {
            val ok = firestoreDataSource.sendCircleReaction(circleId, myUid, toUid, type)
            if (ok) {
                ServiceLocator.analyticsTracker.trackCircleReactionSent(type.name.lowercase())
            } else {
                repository.removeLastReactionTimestamp(toUid)
                _uiState.postValue(_uiState.value!!.copy(errorMessage = "Failed to react"))
            }
        }
    }

    fun clearError() {
        _uiState.postValue(_uiState.value!!.copy(errorMessage = null))
    }

    private fun formatWaitToast(waitMs: Long): String {
        val safe = waitMs.coerceAtLeast(0L)
        val totalSec = (safe + 999L) / 1000L
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return if (mins > 0) "Wait ${mins}m ${secs}s to react again"
        else "Wait ${secs}s to react again"
    }
}

class CircleViewModelFactory(
    private val repository: UsageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CircleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CircleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
