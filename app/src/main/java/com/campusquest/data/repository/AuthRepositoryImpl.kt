package com.campusquest.data.repository

import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    private val _currentUserState = MutableStateFlow<AuthUser?>(firebaseAuth.currentUser?.toDomain())
    override val currentUserState: StateFlow<AuthUser?> = _currentUserState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUserState.value = auth.currentUser?.toDomain()
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user?.toDomain()
                ?: return Result.failure(IllegalStateException("User is null after sign in"))
            _currentUserState.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("User is null after registration"))

            if (displayName.isNotBlank()) {
                val profileUpdates = userProfileChangeRequest {
                    this.displayName = displayName.trim()
                }
                firebaseUser.updateProfile(profileUpdates).await()
            }

            val user = firebaseUser.toDomain()
            _currentUserState.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<AuthUser> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user?.toDomain()
                ?: return Result.failure(IllegalStateException("User is null after anonymous sign in"))
            _currentUserState.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        _currentUserState.value = null
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    private fun FirebaseUser.toDomain(): AuthUser {
        return AuthUser(
            uid = uid,
            displayName = displayName ?: if (isAnonymous) "Guest Explorer" else email?.substringBefore("@"),
            email = email,
            isAnonymous = isAnonymous
        )
    }
}
