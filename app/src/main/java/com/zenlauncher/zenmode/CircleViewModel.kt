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
import com.zenlauncher.zenmode.coreapi.services.Entitlement
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
    /** One-shot: true right after a successful createCircle() / joinCircle() /
     * confirmSwitchFromBuddyAndJoin(). Mirrors [justLeftCircle] -- the host must navigate to
     * the circle dashboard rather than infer success from [circle] transitioning non-null,
     * which is ambiguous (the create/join screens can be reopened while already in a circle).
     * Consume via [CircleViewModel.consumeEnteredCircleEvent]. */
    val justEnteredCircle: Boolean = false,
    /** Pair(loveReceivedToday, meltReceivedToday) -- reactions sent TO you today, from anyone
     * in the circle. See FirestoreDataSource.getTodayCircleReactions. Refreshed by [loadCircle]
     * (which already runs on every circle_react FCM push, same as MainViewModel's
     * buddyReactedEvents -> fetchLikes wiring). There's no sender-side optimistic update here
     * (unlike MainViewModel.sendLike()'s _myLikes bump) -- sending a reaction doesn't change
     * what *you* received, only what the recipient sees on their own client. */
    val reactions: Pair<Long, Long> = 0L to 0L,
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
            val reactions = circle?.let { firestoreDataSource.getTodayCircleReactions(it.id, myUid) } ?: (0L to 0L)
            _uiState.postValue(_uiState.value!!.copy(circle = circle, loading = false, reactions = reactions))
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
                    justEnteredCircle = circle != null,
                    errorMessage = if (circle == null) "Couldn't create circle" else null
                )
            )
        }
    }

    private suspend fun joinCircleInternal(circleId: String, via: String, myUid: String, myHasBuddy: Boolean): CircleJoinResult {
        _uiState.postValue(_uiState.value!!.copy(loading = true))
        val displayName = ServiceLocator.authProvider.getDisplayName()
        val result = firestoreDataSource.joinCircle(circleId, myUid, displayName, confirmedSwitchFromBuddy = false)
        when (result) {
            is CircleJoinResult.Success -> {
                repository.saveCircleId(circleId)
                // Fetch and post directly rather than calling loadCircle() -- that posts its
                // own loading/circle state from a second coroutine, which would race with
                // (and could stomp) the justEnteredCircle=true below.
                val circle = firestoreDataSource.getCircle(circleId)
                ServiceLocator.analyticsTracker.trackZencircleJoinedV3(circleId, via, circle?.members?.size ?: 1)
                if (circle != null) repository.cacheCircle(circle)
                _uiState.postValue(_uiState.value!!.copy(circle = circle, loading = false, justEnteredCircle = circle != null))
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
        return result
    }

    /** [via] is "invite_link" or "code", for analytics. Fire-and-forget -- callers watch
     * [uiState] for the outcome (e.g. the deep-link auto-join path). */
    fun joinCircle(circleId: String, via: String) {
        val myUid = myUid() ?: return
        val myHasBuddy = repository.hasCachedBuddy()
        viewModelScope.launch { joinCircleInternal(circleId, via, myUid, myHasBuddy) }
    }

    /** Suspend variant of [joinCircle] for callers that need the real result synchronously
     * (UseCodeCard's inline status text) instead of watching [uiState]. Same side effects. */
    suspend fun joinCircleAwait(circleId: String, via: String): CircleJoinResult? {
        val myUid = myUid() ?: return null
        val myHasBuddy = repository.hasCachedBuddy()
        return joinCircleInternal(circleId, via, myUid, myHasBuddy)
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
                when (result) {
                    is CircleJoinResult.Success -> {
                        repository.saveCircleId(circleId)
                        // Same reasoning as joinCircle() above -- fetch and post directly instead
                        // of loadCircle() so we can set justEnteredCircle=true in one shot.
                        val circle = firestoreDataSource.getCircle(circleId)
                        ServiceLocator.analyticsTracker.trackZencircleJoinedV3(circleId, "buddy_switch", circle?.members?.size ?: 1)
                        if (circle != null) repository.cacheCircle(circle)
                        _uiState.postValue(_uiState.value!!.copy(circle = circle, loading = false, justEnteredCircle = circle != null, pendingBuddySwitchCircleId = null))
                    }
                    else -> {
                        // Deliberately NOT the usual silent-safe-default here: disconnect already
                        // succeeded, so silence would leave the user thinking they joined when
                        // they're actually in neither Buddy nor Circle. See plan doc's Risks section.
                        _uiState.postValue(
                            _uiState.value!!.copy(
                                loading = false,
                                errorMessage = "Disconnected your buddy, but couldn't join the circle -- please try the invite link again."
                            )
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

    /** Call once the host has navigated to the circle dashboard in response to
     * justEnteredCircle. */
    fun consumeEnteredCircleEvent() {
        _uiState.postValue(_uiState.value!!.copy(justEnteredCircle = false))
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

        // No optimistic update here (unlike MainViewModel.sendLike()'s _myLikes bump) --
        // sending a reaction doesn't change what *you* received, only what the recipient sees
        // on their own client when their circle_react push arrives.
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

    /** Random circle connect -- mirrors BuddyConnector.randomConnect()'s cooldown and weekly
     * quota (shared counter -- a random circle connect and a random buddy connect draw from
     * the same weekly allowance), since it's the same kind of matchmaking action. Creates a
     * real 2-person circle atomically (both people are online right now, unlike the
     * invite-link flow). [isPro] comes from the caller (ProAccess.isPro(context)) -- this
     * ViewModel has no Context to ask itself. */
    fun findRandomCircle(isPro: Boolean) {
        val myUid = myUid() ?: return
        val lastTried = repository.getLastRandomConnectAttemptTime()
        val remaining = AppConstants.RANDOM_CONNECT_COOLDOWN_MS - (System.currentTimeMillis() - lastTried)
        if (remaining > 0) {
            val secs = (remaining / 1000).coerceAtLeast(1)
            _uiState.postValue(_uiState.value!!.copy(errorMessage = "No one was available last time. Try again in ${secs}s."))
            return
        }
        _uiState.postValue(_uiState.value!!.copy(loading = true))
        viewModelScope.launch {
            val weeklyLimit = if (isPro) Entitlement.RANDOM_CONNECT_PRO_WEEKLY_LIMIT else Entitlement.RANDOM_CONNECT_FREE_WEEKLY_LIMIT
            if (!firestoreDataSource.hasRandomConnectQuota(myUid, weeklyLimit)) {
                val message = if (isPro) {
                    "You've used all $weeklyLimit random connects this week. More open up next week."
                } else {
                    "You've used all $weeklyLimit random connects this week. Upgrade to Pro for up to ${Entitlement.RANDOM_CONNECT_PRO_WEEKLY_LIMIT}/week."
                }
                _uiState.postValue(_uiState.value!!.copy(loading = false, errorMessage = message))
                return@launch
            }
            val displayName = ServiceLocator.authProvider.getDisplayName()
            val circle = firestoreDataSource.findRandomCircleUser(myUid, displayName)
            if (circle == null) {
                repository.saveLastRandomConnectAttemptTime(System.currentTimeMillis())
                _uiState.postValue(_uiState.value!!.copy(loading = false, errorMessage = "No one available right now. Try again in 30 seconds!"))
            } else {
                firestoreDataSource.recordRandomConnectUsed(myUid)
                repository.saveCircleId(circle.id)
                repository.cacheCircle(circle)
                ServiceLocator.analyticsTracker.trackCircleJoined("random")
                _uiState.postValue(_uiState.value!!.copy(circle = circle, loading = false, justEnteredCircle = true))
            }
        }
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
