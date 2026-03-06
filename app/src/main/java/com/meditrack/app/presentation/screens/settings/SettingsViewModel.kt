package com.meditrack.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditrack.app.data.local.dao.UserDao
import com.meditrack.app.data.local.entity.UserEntity
import com.meditrack.app.data.preferences.AppPreferences
import com.meditrack.app.data.repository.AuthRepository
import com.meditrack.app.data.repository.DoseLogRepository
import com.meditrack.app.data.repository.MedicineRepository
import com.meditrack.app.data.sync.FirestoreSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val displayName: String = "User",
    val email: String = "",
    val dateOfBirth: Long? = null,
    val notificationsEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 0,
    val themeMode: String = "SYSTEM",
    val fontSize: String = "NORMAL",
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val appPreferences: AppPreferences,
    private val authRepository: AuthRepository,
    private val medicineRepository: MedicineRepository,
    private val doseLogRepository: DoseLogRepository,
    private val firestoreSyncService: FirestoreSyncService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(userDao.getUser(), appPreferences.state) { user, prefs ->
                val u = user ?: UserEntity()
                _uiState.value = SettingsUiState(
                    displayName = u.displayName,
                    email = u.email ?: "",
                    dateOfBirth = u.dateOfBirth,
                    notificationsEnabled = prefs.notificationsEnabled,
                    reminderLeadMinutes = prefs.reminderLeadMinutes,
                    themeMode = prefs.themeMode,
                    fontSize = prefs.fontSize,
                    isLoading = false
                )
            }.collect { }
        }
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value)
        saveProfile()
    }

    fun onDateOfBirthChanged(millis: Long?) {
        _uiState.value = _uiState.value.copy(dateOfBirth = millis)
        saveProfile()
    }

    fun onNotificationsEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        viewModelScope.launch { appPreferences.setNotificationsEnabled(enabled) }
    }

    fun onReminderLeadMinutesChanged(minutes: Int) {
        _uiState.value = _uiState.value.copy(reminderLeadMinutes = minutes)
        viewModelScope.launch { appPreferences.setReminderLeadMinutes(minutes) }
    }

    fun onThemeModeChanged(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun onFontSizeChanged(size: String) {
        _uiState.value = _uiState.value.copy(fontSize = size)
        viewModelScope.launch { appPreferences.setFontSize(size) }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            val user = UserEntity(
                id = 1,
                displayName = state.displayName,
                email = state.email.ifBlank { null },
                dateOfBirth = state.dateOfBirth
            )
            userDao.insertUser(user)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            firestoreSyncService.clearCloudDataForCurrentUser()
            medicineRepository.deleteAllMedicines()
            doseLogRepository.deleteAllDoseLogs()
            userDao.deleteUser()
            userDao.insertUser(UserEntity())
            appPreferences.reset()
            _events.emit(SettingsEvent.DataCleared)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firestoreSyncService.stopRealtimeSync()
            firestoreSyncService.clearLocalSyncData()
            authRepository.signOut()
            _events.emit(SettingsEvent.SignedOut)
        }
    }
}

sealed class SettingsEvent {
    data object DataCleared : SettingsEvent()
    data object SignedOut : SettingsEvent()
}
