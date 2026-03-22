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

data class BuddyStats(
    val screenTimeMins: Long,
    val unlocks: Int
)

class MainViewModel(private val repository: UsageRepository) : ViewModel() {
    private val firestoreDataSource = ServiceLocator.firestoreDataSource

    private val _stats = MutableLiveData<DailyUsage>()
    val stats: LiveData<DailyUsage> get() = _stats

    private val _navigateToDelayedUnlock = MutableLiveData<Boolean>()
    val navigateToDelayedUnlock: LiveData<Boolean> get() = _navigateToDelayedUnlock

    private val _buddyStats = MutableLiveData<BuddyStats>()
    val buddyStats: LiveData<BuddyStats> get() = _buddyStats

    private val _hasBuddies = MutableLiveData<Boolean>()
    val hasBuddies: LiveData<Boolean> get() = _hasBuddies

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
    }

    fun onScreenUnlocked() {
        val now = System.currentTimeMillis()
        if (now - lastUnlockTimestamp < UNLOCK_DEBOUNCE_MS) return // Debounce rapid duplicate broadcasts
        lastUnlockTimestamp = now

        sessionStartTime = now
        repository.incrementUnlockCount()
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
        _stats.value = repository.getTodayUsage()
    }

    /** Refresh buddy stats UI from SharedPreferences cache only — no Firestore calls. */
    fun refreshBuddyStatsFromCache() {
        if (repository.hasCachedBuddy()) {
            val (cachedTime, cachedUnlocks) = repository.getBuddyStats()
            _hasBuddies.postValue(true)
            _buddyStats.postValue(BuddyStats(cachedTime, cachedUnlocks))
        }
    }

    fun fetchBuddyData() {
        val myUid = repository.getUserUid()
            ?: ServiceLocator.authProvider.getCurrentUserId()
            ?: return

        viewModelScope.launch {
            // Load cache immediately for instant UI
            val (cachedTime, cachedUnlocks) = repository.getBuddyStats()
            if (repository.hasCachedBuddy()) {
                 _hasBuddies.postValue(true)
                 _buddyStats.postValue(BuddyStats(cachedTime, cachedUnlocks))
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
                        screenTimeMins = stat.screenTime,
                        unlocks = stat.unlocks
                    )
                    
                    // Save to cache
                    repository.saveBuddyStats(buddyStats.screenTimeMins, buddyStats.unlocks)
                    
                    _buddyStats.postValue(buddyStats)
                }
            } catch (e: Exception) {
                // On failure, still check if we have cached buddy
                _hasBuddies.postValue(repository.hasCachedBuddy())
            }
        }
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
