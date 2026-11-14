package com.campusquest.ui.common

import com.campusquest.CampusQuestApplication
import com.campusquest.data.mock.MockAuthRepository
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository

/** Single place where M1 ViewModels get their repositories. Swap the auth line when M5's real repo is ready. */
object AppProviders {
    val gameRepository: GameRepository
        get() = CampusQuestApplication.instance.gameRepository

    val authRepository: AuthRepository by lazy { MockAuthRepository() }
}