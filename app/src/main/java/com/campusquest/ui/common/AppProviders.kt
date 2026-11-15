package com.campusquest.ui.common

import android.util.Log
import com.campusquest.CampusQuestApplication
import com.campusquest.data.mock.MockAuthRepository
import com.campusquest.data.repository.AuthRepositoryImpl
import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository
import com.google.firebase.auth.FirebaseAuth

/** Single place where ViewModels and Fragments get their shared repositories. */
object AppProviders {

    val gameRepository: GameRepository
        get() = CampusQuestApplication.instance.gameRepository

    /**
     * Uses the single app-wide AuthRepository from CampusQuestApplication.
     * Falls back to MockAuthRepository only when Firebase cannot start at all.
     */
    val authRepository: AuthRepository by lazy {
        try {
            CampusQuestApplication.instance.authRepository ?: createFallbackAuthRepository()
        } catch (e: Exception) {
            createFallbackAuthRepository()
        }
    }

    fun currentUserId(): String {
        return authRepository.getCurrentUserId()?.takeIf { it.isNotBlank() }
            ?: GameRepositoryImpl.DEFAULT_LOCAL_USER_ID
    }

    private fun createFallbackAuthRepository(): AuthRepository = try {
        AuthRepositoryImpl(FirebaseAuth.getInstance())
    } catch (e: Exception) {
        Log.w("AppProviders", "Firebase Auth is unavailable, using mock auth", e)
        MockAuthRepository()
    }
}
