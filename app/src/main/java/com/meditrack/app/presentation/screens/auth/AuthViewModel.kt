package com.meditrack.app.presentation.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meditrack.app.data.repository.AuthRepository
import com.meditrack.app.data.sync.FirestoreSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val firebaseEnabled: Boolean = false
)

sealed class AuthEvent {
    data object Success : AuthEvent()
    data class Error(val message: String) : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestoreSyncService: FirestoreSyncService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(firebaseEnabled = authRepository.isFeatureEnabled))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
    }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.signInWithEmail(state.email.trim(), state.password)
            result.fold(
                onSuccess = {
                    firestoreSyncService.pullFromCloud()
                    firestoreSyncService.pushAllToCloud()
                    _events.emit(AuthEvent.Success)
                },
                onFailure = { _events.emit(AuthEvent.Error(it.message ?: "Sign in failed")) }
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password are required")
            return
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(errorMessage = "Password must be at least 6 characters")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(errorMessage = "Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.registerWithEmail(state.email.trim(), state.password)
            result.fold(
                onSuccess = {
                    firestoreSyncService.pullFromCloud()
                    firestoreSyncService.pushAllToCloud()
                    _events.emit(AuthEvent.Success)
                },
                onFailure = { _events.emit(AuthEvent.Error(it.message ?: "Registration failed")) }
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = {
                    firestoreSyncService.pullFromCloud()
                    firestoreSyncService.pushAllToCloud()
                    _events.emit(AuthEvent.Success)
                },
                onFailure = { _events.emit(AuthEvent.Error(it.message ?: "Google sign in failed")) }
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun signInWithGoogleInBrowser(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.signInWithGoogleInBrowser(activity)
            result.fold(
                onSuccess = {
                    firestoreSyncService.pullFromCloud()
                    firestoreSyncService.pushAllToCloud()
                    _events.emit(AuthEvent.Success)
                },
                onFailure = { _events.emit(AuthEvent.Error(it.message ?: "Google web sign in failed")) }
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
