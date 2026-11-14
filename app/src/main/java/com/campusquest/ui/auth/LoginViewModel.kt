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
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = AppProviders.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AuthUser>>(UiState.Idle)
    val uiState: StateFlow<UiState<AuthUser>> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = UiState.Error("Please enter email and password")
            return
        }
        submit { authRepository.signInWithEmail(email.trim(), password) }
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