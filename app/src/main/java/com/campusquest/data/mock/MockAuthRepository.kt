package com.campusquest.data.mock

import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Temporary fake auth so the login UI works before Firebase Auth (M5) is available. */
class MockAuthRepository : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUserState: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        delay(600)
        if (!email.contains("@") || password.length < 6) {
            return Result.failure(
                IllegalArgumentException("Enter a valid email and a password of at least 6 characters")
            )
        }
        val user = AuthUser(
            uid = "mock-${email.lowercase().hashCode()}",
            displayName = email.substringBefore("@"),
            email = email
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> {
        delay(600)
        val user = AuthUser(uid = "mock-${email.lowercase().hashCode()}", displayName = displayName, email = email)
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signInAnonymously(): Result<AuthUser> {
        delay(300)
        val user = AuthUser(uid = "mock-guest", displayName = "Guest Explorer", email = null, isAnonymous = true)
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }

    override fun getCurrentUserId(): String? = _currentUser.value?.uid
}