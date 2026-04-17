package com.zenlauncher.zenmode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BuddyStats(
    val screenTimeMins: Long
)

class MainViewModel(private val repository: UsageRepository) : ViewModel() {
    private val firestoreDataSource = ServiceLocator.firestoreDataSource

    private val _stats = MutableLiveData<DailyUsage>()
    val stats: LiveData<DailyUsage> get() = _stats

    private val _navigateToDelayedUnlock = MutableLiveData<Boolean>()
    val navigateToDelayedUnlock: LiveData<Boolean> get() = _navigateToDelayedUnlock

    private val _yesterdayChangePercent = MutableLiveData<Int?>()
    val yesterdayChangePercent: LiveData<Int?> get() = _yesterdayChangePercent

    private val _buddyStats = MutableLiveData<BuddyStats>()
    val buddyStats: LiveData<BuddyStats> get() = _buddyStats

    private val _hasBuddies = MutableLiveData<Boolean>()
    val hasBuddies: LiveData<Boolean> get() = _hasBuddies

    private val _showForceUpdateDialog = MutableStateFlow(false)
    val showForceUpdateDialog: StateFlow<Boolean> = _showForceUpdateDialog.asStateFlow()

    private val _myLikes = MutableLiveData<Long>(0L)
    val myLikes: LiveData<Long> get() = _myLikes

    private val _buddyLikes = MutableLiveData<Long>(0L)
    val buddyLikes: LiveData<Long> get() = _buddyLikes

    private val _likeToast = MutableLiveData<String?>()
    val likeToast: LiveData<String?> get() = _likeToast

    private var sessionStartTime: Long = 0
    private var hasTriggeredDelayedUnlock = false
    private var lastUnlockTimestamp = 0L
    private val UNLOCK_DEBOUNCE_MS = 2000L

    init {
        // Reset zen unlock flag on fresh process start.
        // When the OS kills our process, ACTION_SCREEN_OFF is never received,
        // leaving is_zen_unlocked stuck at true in SharedPrefs. This ensures
        // the resistance screen shows on every fresh launch.
        repository.setZenUnlockFlag(false)
        refreshStats()

        // Collect remote config state immediately (reacts to cache before fetch completes)
        viewModelScope.launch {
            ServiceLocator.remoteConfigProvider.minVersionCode.collect { minVersion ->
                // App's version code is generated in BuildConfig
                val currentVersion = BuildConfig.VERSION_CODE
                _showForceUpdateDialog.value = (minVersion > currentVersion) && !repository.isForceUpdateSnoozed()
            }
        }

        // Setup remote config for forced updates independently
        viewModelScope.launch {
            try {
                ServiceLocator.remoteConfigProvider.initialize()
            } catch (e: Exception) {
                // If it fails, we fall back to not blocking
            }
        }

        // Live-refresh likes when a buddy_react FCM push arrives while app is foregrounded.
        viewModelScope.launch {
            ServiceLocator.buddyReactedEvents.collect {
                val myUid = repository.getUserUid() ?: return@collect
                val buddyUid = repository.getBuddyUid() ?: return@collect
                fetchLikes(myUid, buddyUid)
            }
        }
    }

    fun onScreenUnlocked() {
        val now = System.currentTimeMillis()
        if (now - lastUnlockTimestamp < UNLOCK_DEBOUNCE_MS) return // Debounce rapid duplicate broadcasts
        lastUnlockTimestamp = now

        sessionStartTime = now
        refreshStats()
        if (!hasTriggeredDelayedUnlock) {
            _navigateToDelayedUnlock.value = true
            hasTriggeredDelayedUnlock = true
        }
    }

    fun onScreenLocked() {
        repository.setZenUnlockFlag(false)
        hasTriggeredDelayedUnlock = false
        if (sessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            repository.updateScreenTime(sessionDuration)
            sessionStartTime = 0
            refreshStats()
        }
    }

    fun onResumeCheck() {
         if (!repository.isZenUnlocked() && !hasTriggeredDelayedUnlock) {
             _navigateToDelayedUnlock.value = true
             hasTriggeredDelayedUnlock = true
         }
         // Flush elapsed screen time from the active session so the UI stays live
         if (sessionStartTime > 0) {
             val elapsed = System.currentTimeMillis() - sessionStartTime
             repository.updateScreenTime(elapsed)
             sessionStartTime = System.currentTimeMillis() // reset checkpoint
         }
         refreshStats()
    }

    // Reset the navigation event after it's handled
    fun onDelayedUnlockNavigated() {
        _navigateToDelayedUnlock.value = false
    }

    fun refreshStats() {
        val todayUsage = repository.getTodayUsage()
        _stats.value = todayUsage

        val yesterdayMillis = repository.getYesterdayScreenTimeMillis()
        _yesterdayChangePercent.value = if (yesterdayMillis > 0) {
            (((todayUsage.screenTimeInMillis - yesterdayMillis).toDouble() / yesterdayMillis) * 100).toInt()
        } else {
            null
        }
    }

    fun snoozeForceUpdateUntilTomorrow() {
        repository.snoozeForceUpdate()
        _showForceUpdateDialog.value = false
    }

    fun refreshBuddyStatsFromCache() {
        if (repository.hasCachedBuddy()) {
            val cachedTime = repository.getBuddyScreenTime()
            _hasBuddies.postValue(true)
            _buddyStats.postValue(BuddyStats(cachedTime))
        } else {
            _hasBuddies.postValue(false)
        }
    }

    fun fetchBuddyData() {
        val myUid = repository.getUserUid()
            ?: ServiceLocator.authProvider.getCurrentUserId()
            ?: return

        viewModelScope.launch {
            // Load cache immediately for instant UI
            if (repository.hasCachedBuddy()) {
                val cachedTime = repository.getBuddyScreenTime()
                 _hasBuddies.postValue(true)
                 _buddyStats.postValue(BuddyStats(cachedTime))
            }

            try {
                // Step 1: Get single buddy UID (from cache or Firestore)
                val buddyUid: String?
                if (repository.hasCachedBuddy()) {
                    buddyUid = repository.getBuddyUid()
                } else {
                    buddyUid = firestoreDataSource.getBuddyUid(myUid)
                    if (buddyUid != null) {
                        repository.saveBuddyUid(buddyUid)
                    }
                }

                _hasBuddies.postValue(buddyUid != null)

                if (buddyUid == null) return@launch

                // Step 2: Fetch stats for the single buddy
                val stat = firestoreDataSource.getBuddyStats(buddyUid)

                if (stat != null) {
                    val buddyStats = BuddyStats(
                        screenTimeMins = stat.screenTime
                    )

                    // Save to cache
                    repository.saveBuddyScreenTime(buddyStats.screenTimeMins)

                    _buddyStats.postValue(buddyStats)
                }

                fetchLikes(myUid, buddyUid)
            } catch (e: Exception) {
                // On failure, still check if we have cached buddy
                _hasBuddies.postValue(repository.hasCachedBuddy())
            }
        }
    }

    private suspend fun fetchLikes(myUid: String, buddyUid: String) {
        try {
            val relId = firestoreDataSource.getRelationshipId(myUid, buddyUid)
            val (mine, buddy) = firestoreDataSource.getTodayLikes(relId, myUid, buddyUid)
            _myLikes.postValue(mine)
            _buddyLikes.postValue(buddy)
        } catch (_: Exception) {
        }
    }

    fun sendLike() {
        val myUid = repository.getUserUid()
            ?: ServiceLocator.authProvider.getCurrentUserId()
            ?: return
        val buddyUid = repository.getBuddyUid() ?: return

        val recent = repository.getRecentLikeTimestamps()
        if (recent.size >= UsageRepository.LIKE_MAX_COUNT) {
            val oldest = recent.first()
            val waitMs = (oldest + UsageRepository.LIKE_WINDOW_MS) - System.currentTimeMillis()
            _likeToast.postValue(formatWaitToast(waitMs))
            return
        }

        val previous = _myLikes.value ?: 0L
        _myLikes.postValue(previous + 1)
        repository.recordLikeSent()

        viewModelScope.launch {
            val relId = firestoreDataSource.getRelationshipId(myUid, buddyUid)
            val ok = firestoreDataSource.sendLike(relId, myUid)
            if (!ok) {
                _myLikes.postValue(previous)
                repository.removeLastLikeTimestamp()
                _likeToast.postValue("Failed to react")
            }
        }
    }

    fun clearLikeToast() {
        _likeToast.value = null
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

class MainViewModelFactory(private val repository: UsageRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
