package com.campusquest.data.repository

import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.model.AuthUser
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth
) : AuthRepository {
    private val _currentUserState = MutableStateFlow<AuthUser?>(null)

    override val currentUserState: StateFlow<AuthUser?> = _currentUserState

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _currentUserState.value = AuthUser(
                    uid = user.uid,
                    displayName = user.displayName,
                    email = user.email,
                    isAnonymous = user.isAnonymous
                )
            } else {
                _currentUserState.value = null
            }
        }
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<AuthUser> {
        return try {

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            val authUser = AuthUser(
                uid = firebaseUser!!.uid,
                displayName = firebaseUser.displayName,
                email = firebaseUser.email,
                isAnonymous = firebaseUser.isAnonymous
            )

            Result.success(authUser)
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

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            val authUser = AuthUser(
                uid = firebaseUser!!.uid,
                displayName = displayName,
                email = firebaseUser.email,
                isAnonymous = firebaseUser.isAnonymous
            )

            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<AuthUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val firebaseUser = result.user

            val authUser = AuthUser(
                uid = firebaseUser!!.uid,
                displayName = "Guest",
                email = null,
                isAnonymous = true
            )

            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

}