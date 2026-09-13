package com.campusquest.domain.repository

import com.campusquest.domain.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserState: StateFlow<AuthUser?>

    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<AuthUser>

    suspend fun signInAnonymously(): Result<AuthUser>

    suspend fun signOut()

    fun getCurrentUserId(): String?
}
