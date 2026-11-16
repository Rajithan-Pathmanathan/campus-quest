package com.campusquest.data.repository

import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    /** Runs after every successful sign-in / sign-up (profile document, FCM token, sync). */
    private val onSignedIn: suspend (AuthUser) -> Unit = {},
    /** Removes the user's cloud data before the account itself is deleted. */
    private val deleteUserData: suspend (uid: String) -> Unit = {}
) : AuthRepository {

    private val _currentUserState = MutableStateFlow(auth.currentUser?.toDomain())
    override val currentUserState: StateFlow<AuthUser?> = _currentUserState

    init {
        auth.addAuthStateListener { _currentUserState.value = it.currentUser?.toDomain() }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> = attempt {
        val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign-in did not return a user")
        signedIn(user.toDomain())
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<AuthUser> = attempt {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign-up did not return a user")
        val name = displayName.trim()
        if (name.isNotEmpty()) {
            user.updateProfile(userProfileChangeRequest { this.displayName = name }).await()
            user.reload().await()
        }
        signedIn(user.toDomain().copy(displayName = name.ifEmpty { user.toDomain().displayName }))
    }

    override suspend fun signInAnonymously(): Result<AuthUser> = attempt {
        val user = auth.signInAnonymously().await().user ?: error("Guest sign-in did not return a user")
        signedIn(user.toDomain())
    }

    override suspend fun signOut() {
        auth.signOut()
        _currentUserState.value = null
    }

    override suspend fun deleteAccount(): Result<Unit> = attempt {
        val user = auth.currentUser ?: error("No one is signed in")
        deleteUserData(user.uid)
        user.delete().await()
        _currentUserState.value = null
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    private suspend fun signedIn(user: AuthUser): AuthUser {
        _currentUserState.value = user
        runCatching { onSignedIn(user) } // profile/token sync must never block sign-in
        return user
    }

    private inline fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        android.util.Log.w("AuthRepository", "Auth failed: ${(e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName}")
        Result.failure(Exception(friendlyMessage(e), e))
    }

    private fun FirebaseUser.toDomain() = AuthUser(
        uid = uid,
        displayName = displayName?.takeIf { it.isNotBlank() } ?: if (isAnonymous) "Guest Explorer" else email?.substringBefore("@"),
        email = email,
        isAnonymous = isAnonymous
    )

    companion object {
        /** Short, readable messages instead of raw Firebase errors. */
        fun friendlyMessage(e: Exception): String = when (e) {
            is FirebaseNetworkException -> "No internet connection. Check your connection and try again."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a moment and try again."
            is FirebaseAuthWeakPasswordException -> "Please choose a stronger password (at least 6 characters)."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Try signing in."
            is FirebaseAuthInvalidUserException -> "No account found for this email, or it has been disabled."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthRecentLoginRequiredException -> "For your security, please sign out, sign in again and then retry."
            is FirebaseAuthException -> when (e.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED", "ERROR_ADMIN_RESTRICTED_OPERATION" ->
                    "This sign-in method is not enabled for Campus Quest yet. Please use email sign-in or try later."
                else -> "Sign-in failed (${e.errorCode}). Please try again."
            }
            else -> e.message ?: "Something went wrong. Please try again."
        }
    }
}
