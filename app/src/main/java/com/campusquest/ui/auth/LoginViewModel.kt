package com.campusquest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.ui.common.AppProviders
import com.campusquest.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = AppProviders.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AuthUser>>(UiState.Idle)
    val uiState: StateFlow<UiState<AuthUser>> = _uiState.asStateFlow()

    private val _isRegisterMode = MutableStateFlow(false)
    val isRegisterMode: StateFlow<Boolean> = _isRegisterMode.asStateFlow()

    init {
        // Firebase may deliver its persisted session after this ViewModel is created.
        viewModelScope.launch {
            val user = authRepository.currentUserState.filterNotNull().first()
            _uiState.value = UiState.Success(user)
        }
    }

    fun toggleMode() {
        if (_uiState.value is UiState.Loading) return
        _isRegisterMode.value = !_isRegisterMode.value
        _uiState.value = UiState.Idle
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = UiState.Error("Please enter email and password")
            return
        }
        submit { authRepository.signInWithEmail(email.trim(), password) }
    }

    fun register(email: String, password: String, displayName: String) {
        when {
            displayName.isBlank() ->
                _uiState.value = UiState.Error("Please enter your name")
            !email.contains("@") ->
                _uiState.value = UiState.Error("Please enter a valid email address")
            password.length < 6 ->
                _uiState.value = UiState.Error("Password must be at least 6 characters")
            else ->
                submit { authRepository.signUpWithEmail(email.trim(), password, displayName.trim()) }
        }
    }

    fun continueAsGuest() = submit { authRepository.signInAnonymously() }

    private fun submit(block: suspend () -> Result<AuthUser>) {
        if (_uiState.value is UiState.Loading) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            _uiState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Sign-in failed") }
            )
        }
    }
}
