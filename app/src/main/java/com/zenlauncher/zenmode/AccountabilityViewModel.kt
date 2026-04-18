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

data class AccountabilityUiState(
    val myUsage: DailyUsage = DailyUsage(0L),
    val buddyStats: BuddyStats? = null,
    val userCode: String? = null,
    val buddyUid: String? = null,
    val connectionDateMillis: Long? = null,
    val disconnectResult: DisconnectResult? = null
)

sealed class DisconnectResult {
    data object Success : DisconnectResult()
    data class Error(val message: String) : DisconnectResult()
}

class AccountabilityViewModel(private val repository: UsageRepository) : ViewModel() {

    private val firestoreDataSource = ServiceLocator.firestoreDataSource

    private val _uiState = MutableLiveData(AccountabilityUiState())
    val uiState: LiveData<AccountabilityUiState> get() = _uiState

    private val _myLikes = MutableLiveData<Long>(0L)
    val myLikes: LiveData<Long> get() = _myLikes

    private val _buddyLikes = MutableLiveData<Long>(0L)
    val buddyLikes: LiveData<Long> get() = _buddyLikes

    private val _likeToast = MutableLiveData<String?>()
    val likeToast: LiveData<String?> get() = _likeToast

    init {
        loadData()

        // Live-refresh likes when a buddy_react FCM push arrives while app is foregrounded.
        viewModelScope.launch {
            ServiceLocator.buddyReactedEvents.collect {
                val myUid = repository.getUserUid()
                    ?: ServiceLocator.authProvider.getCurrentUserId()
                    ?: return@collect
                val buddyUid = repository.getBuddyUid() ?: return@collect
                val relId = firestoreDataSource.getRelationshipId(myUid, buddyUid)
                val (mine, buddy) = firestoreDataSource.getTodayLikes(relId, myUid, buddyUid)
                _myLikes.postValue(mine)
                _buddyLikes.postValue(buddy)
            }
        }
    }

    private fun loadData() {
        // Sum 7-day screen time for weekly view
        val weeklyTotalMillis = repository.getWeeklyScreenTimeMillis().sum()
        val myUsage = DailyUsage(weeklyTotalMillis)

        val buddyStats = if (repository.hasCachedBuddy()) BuddyStats(repository.getBuddyScreenTime()) else null

        val userCode = repository.getUserUid()
            ?: ServiceLocator.authProvider.getCurrentUserId()
        val buddyUid = repository.getBuddyUid()

        _uiState.value = AccountabilityUiState(
            myUsage = myUsage,
            buddyStats = buddyStats,
            userCode = userCode,
            buddyUid = buddyUid,
            connectionDateMillis = repository.getBuddyConnectionDate()
        )

        // Fetch connection date from Firestore if not cached
        if (repository.getBuddyConnectionDate() == null && userCode != null) {
            viewModelScope.launch {
                val fetched = firestoreDataSource.getRelationshipCreatedAt(userCode)
                if (fetched != null) {
                    repository.saveBuddyConnectionDate(fetched)
                    _uiState.postValue(_uiState.value!!.copy(connectionDateMillis = fetched))
                }
            }
        }

        if (userCode != null && buddyUid != null) {
            viewModelScope.launch {
                val relId = firestoreDataSource.getRelationshipId(userCode, buddyUid)
                val (mine, buddy) = firestoreDataSource.getTodayLikes(relId, userCode, buddyUid)
                _myLikes.postValue(mine)
                _buddyLikes.postValue(buddy)
            }
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

    fun reload() {
        loadData()
    }

    fun resetDisconnectResult() {
        _uiState.postValue(_uiState.value!!.copy(disconnectResult = null))
    }

    fun disconnectBuddy() {
        val myUid = repository.getUserUid()
            ?: ServiceLocator.authProvider.getCurrentUserId()
            ?: return
        val buddyUid = repository.getBuddyUid() ?: return

        viewModelScope.launch {
            try {
                // disconnectBuddy silently no-ops if relationship docs are already gone
                firestoreDataSource.disconnectBuddy(myUid, buddyUid)
                repository.clearCachedBuddy()
                _uiState.postValue(_uiState.value!!.copy(disconnectResult = DisconnectResult.Success))
            } catch (e: Exception) {
                _uiState.postValue(
                    _uiState.value!!.copy(
                        disconnectResult = DisconnectResult.Error(e.message ?: "Unknown error")
                    )
                )
            }
        }
    }
}

class AccountabilityViewModelFactory(
    private val repository: UsageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountabilityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountabilityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
