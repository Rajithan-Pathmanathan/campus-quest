package com.campusquest.ui.common

import com.campusquest.CampusQuestApplication
import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository

object AppProviders {
    val gameRepository: GameRepository get() = CampusQuestApplication.instance.gameRepository
    val authRepository: AuthRepository get() = requireNotNull(CampusQuestApplication.instance.authRepository) { "Authentication is unavailable" }
    fun currentUserId(): String = authRepository.getCurrentUserId() ?: GameRepositoryImpl.DEFAULT_LOCAL_USER_ID
}
