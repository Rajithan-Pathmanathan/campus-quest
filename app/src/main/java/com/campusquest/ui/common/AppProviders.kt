package com.campusquest.ui.common

import android.util.Log
import com.campusquest.CampusQuestApplication
import com.campusquest.data.mock.MockAuthRepository
import com.campusquest.data.repository.AuthRepositoryImpl
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository
import com.google.firebase.auth.FirebaseAuth

/** Single place where M1 ViewModels get their repositories. */
object AppProviders {

    val gameRepository: GameRepository
        get() = CampusQuestApplication.instance.gameRepository

    /**
     * Uses M5's Firebase auth. The mock is used only when Firebase cannot start at all
     * (for example, google-services.json is missing), never when a sign-in fails.
     */
    val authRepository: AuthRepository by lazy { createAuthRepository() }

    private fun createAuthRepository(): AuthRepository = try {
        AuthRepositoryImpl(FirebaseAuth.getInstance())
    } catch (e: Exception) {
        Log.w("AppProviders", "Firebase Auth is unavailable, using mock auth", e)
        MockAuthRepository()
    }
}
