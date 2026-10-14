package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.FamilyRepository
import com.example.data.repository.LocationRepository
import com.example.data.repository.UserRepository
import com.example.location.LocationTrackingManager
import com.example.model.Family
import com.example.model.FamilyMember
import com.example.model.LocationEvent
import com.example.model.TrackingState
import com.example.model.UserProfile
import com.example.sync.SyncManager
import com.example.sync.SyncStatus
import com.example.sync.SyncWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val locationRepository = LocationRepository(db.locationDao(), db.familyDao())
    val familyRepository = FamilyRepository(db.familyDao())
    val userRepository = UserRepository(db.userDao())
    val trackingManager = LocationTrackingManager(application)
    val syncManager = SyncManager(application, locationRepository)

    val trackingState: StateFlow<TrackingState> = trackingManager.trackingState
    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    val userProfile: StateFlow<UserProfile?> = userRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentFamily: StateFlow<Family?> = userProfile.flatMapLatest { profile ->
        if (profile?.familyId != null) {
            familyRepository.observeFamily(profile.familyId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val familyMembers: StateFlow<List<FamilyMember>> = userProfile.flatMapLatest { profile ->
        if (profile?.familyId != null) {
            familyRepository.observeMembers(profile.familyId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestLocation: StateFlow<LocationEvent?> = userProfile.flatMapLatest { profile ->
        if (profile != null) {
            locationRepository.observeLatestLocation(profile.uid)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val locationHistory: StateFlow<List<LocationEvent>> = userProfile.flatMapLatest { profile ->
        if (profile != null) {
            locationRepository.observeHistory(profile.uid, limit = 100)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        SyncWorker.schedulePeriodicSync(application)
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun signIn(displayName: String) {
        viewModelScope.launch {
            try {
                userRepository.getOrCreateDefaultUser(displayName)
            } catch (e: Exception) {
                _uiMessage.value = "Failed to sign in: ${e.message}"
            }
        }
    }

    fun createFamily(familyName: String) {
        val user = userProfile.value ?: return
        if (familyName.isBlank()) {
            _uiMessage.value = "Please enter a family name"
            return
        }
        viewModelScope.launch {
            try {
                val family = familyRepository.createFamily(familyName, user.uid, user.displayName)
                userRepository.setFamilyId(user.uid, family.familyId)
                _uiMessage.value = "Created family: ${family.name}"
            } catch (e: Exception) {
                _uiMessage.value = "Failed to create family: ${e.message}"
            }
        }
    }

    fun joinFamily(inviteCode: String) {
        val user = userProfile.value ?: return
        if (inviteCode.isBlank()) {
            _uiMessage.value = "Please enter a valid invitation code"
            return
        }
        viewModelScope.launch {
            val result = familyRepository.joinFamilyByInviteCode(inviteCode, user.uid, user.displayName)
            result.onSuccess { family ->
                userRepository.setFamilyId(user.uid, family.familyId)
                _uiMessage.value = "Joined ${family.name}!"
            }.onFailure { err ->
                _uiMessage.value = err.message ?: "Failed to join family"
            }
        }
    }

    fun grantConsent() {
        val user = userProfile.value ?: return
        viewModelScope.launch {
            userRepository.updateConsent(user.uid, true)
            val familyId = user.familyId
            if (familyId != null) {
                trackingManager.startTracking(user.uid, familyId)
            }
        }
    }

    fun startSharing() {
        val user = userProfile.value ?: return
        val familyId = user.familyId ?: run {
            _uiMessage.value = "Please create or join a family first"
            return
        }
        viewModelScope.launch {
            userRepository.updateConsent(user.uid, true)
            trackingManager.startTracking(user.uid, familyId)
            _uiMessage.value = "Location sharing started (updates every 2 minutes)"
        }
    }

    fun grantConsentAndStartSharing() {
        grantConsent()
    }

    fun stopSharing() {
        val user = userProfile.value ?: return
        viewModelScope.launch {
            trackingManager.stopTracking()
            familyRepository.updateMemberSharing(user.uid, false)
            _uiMessage.value = "Location sharing stopped"
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            val count = syncManager.syncPendingEvents()
            _uiMessage.value = if (count > 0) "Synchronized $count queued locations" else "All locations are up to date"
        }
    }

    fun clearLocalHistory() {
        val user = userProfile.value ?: return
        viewModelScope.launch {
            locationRepository.clearHistoryForUser(user.uid)
            _uiMessage.value = "Local history cleared"
        }
    }

    fun updateRetentionDays(days: Int) {
        val user = userProfile.value ?: return
        viewModelScope.launch {
            userRepository.updateRetention(user.uid, days)
            val purged = locationRepository.purgeSyncedRecordsOlderThan(days)
            _uiMessage.value = "Retention set to $days days (purged $purged older records)"
        }
    }

    fun leaveFamily() {
        val user = userProfile.value ?: return
        viewModelScope.launch {
            stopSharing()
            familyRepository.removeMember(user.uid)
            userRepository.setFamilyId(user.uid, null)
            _uiMessage.value = "Left family"
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.cleanup()
    }
}
