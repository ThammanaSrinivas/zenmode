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

    init {
        loadData()
    }

    private fun loadData() {
        // Sum 7-day screen time for weekly view
        val weeklyTotalMillis = repository.getWeeklyScreenTimeMillis().sum()
        val myUsage = DailyUsage(weeklyTotalMillis)

        val buddyStats = if (repository.hasCachedBuddy()) BuddyStats(0) else null

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
