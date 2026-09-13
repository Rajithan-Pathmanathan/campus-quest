package com.campusquest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    val currentUser: StateFlow<AuthUser?> = authRepository.currentUserState

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithEmail(email, pass)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToDashboard)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to sign in.")
                }
            )
        }
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signUpWithEmail(email, pass, displayName)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToDashboard)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to register account.")
                }
            )
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInAnonymously()
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToDashboard)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to enter as guest.")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    sealed interface AuthNavigationEvent {
        object NavigateToDashboard : AuthNavigationEvent
    }
}
