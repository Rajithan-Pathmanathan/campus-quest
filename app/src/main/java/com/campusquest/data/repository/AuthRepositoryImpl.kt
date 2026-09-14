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
    private val customFirebaseAuth: FirebaseAuth? = null
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth? by lazy {
        if (customFirebaseAuth != null) return@lazy customFirebaseAuth
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _currentUserState = MutableStateFlow<AuthUser?>(null)
    override val currentUserState: StateFlow<AuthUser?> = _currentUserState.asStateFlow()

    init {
        try {
            val auth = firebaseAuth
            if (auth != null) {
                _currentUserState.value = auth.currentUser?.toDomain()
                auth.addAuthStateListener { listenerAuth ->
                    _currentUserState.value = listenerAuth.currentUser?.toDomain()
                }
            }
        } catch (e: Exception) {
            // Safe fallback in offline / test environments
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        val auth = firebaseAuth
        return if (auth != null) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val user = authResult.user?.toDomain()
                    ?: return Result.failure(IllegalStateException("User is null after sign in"))
                _currentUserState.value = user
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // Local offline / mock authentication fallback
            val user = AuthUser(
                uid = "offline_user_${email.trim().hashCode()}",
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email.trim(),
                isAnonymous = false
            )
            _currentUserState.value = user
            Result.success(user)
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> {
        val auth = firebaseAuth
        return if (auth != null) {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
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
        } else {
            val user = AuthUser(
                uid = "offline_user_${email.trim().hashCode()}",
                displayName = displayName.trim().ifBlank { email.substringBefore("@") },
                email = email.trim(),
                isAnonymous = false
            )
            _currentUserState.value = user
            Result.success(user)
        }
    }

    override suspend fun signInAnonymously(): Result<AuthUser> {
        val auth = firebaseAuth
        return if (auth != null) {
            try {
                val authResult = auth.signInAnonymously().await()
                val user = authResult.user?.toDomain()
                    ?: return Result.failure(IllegalStateException("User is null after anonymous sign in"))
                _currentUserState.value = user
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val guest = AuthUser(
                uid = "guest_cadet_${System.currentTimeMillis() % 10000}",
                displayName = "Guest Explorer",
                email = null,
                isAnonymous = true
            )
            _currentUserState.value = guest
            Result.success(guest)
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        _currentUserState.value = null
    }

    override fun getCurrentUserId(): String? {
        return try {
            firebaseAuth?.currentUser?.uid ?: _currentUserState.value?.uid
        } catch (e: Exception) {
            _currentUserState.value?.uid
        }
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
