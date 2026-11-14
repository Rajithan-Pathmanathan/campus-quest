package com.campusquest.ui.common

import com.campusquest.CampusQuestApplication
import com.campusquest.data.mock.MockAuthRepository
import com.campusquest.data.repository.AuthRepositoryImpl
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * Single place where M1 ViewModels get their repositories.
 * Set USE_REAL_AUTH = true once M5 confirms Firebase Auth (Email/Password + Anonymous) is enabled.
 */
object AppProviders {

    private const val USE_REAL_AUTH = false

    val gameRepository: GameRepository
        get() = CampusQuestApplication.instance.gameRepository

    val authRepository: AuthRepository by lazy {
        if (USE_REAL_AUTH) AuthRepositoryImpl(FirebaseAuth.getInstance()) else MockAuthRepository()
    }
}