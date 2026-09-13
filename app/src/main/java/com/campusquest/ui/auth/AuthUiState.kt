package com.campusquest.ui.auth

import com.campusquest.domain.model.AuthUser

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Authenticated(val user: AuthUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
