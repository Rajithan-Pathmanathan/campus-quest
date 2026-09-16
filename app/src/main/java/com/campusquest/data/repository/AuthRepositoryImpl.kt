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
    override val currentUserState: StateFlow<AuthUser?>
        get() = TODO("Not yet implemented")

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<AuthUser> {
        return try {

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            // 3. TODO: Convert firebaseUser into your app's AuthUser model
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
        TODO("Not yet implemented")
    }

    override suspend fun signOut() {
        TODO("Not yet implemented")
    }

    override fun getCurrentUserId(): String? {
        TODO("Not yet implemented")
    }

}