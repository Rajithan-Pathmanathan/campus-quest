package com.campusquest.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusquest.CampusQuestApplication
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository
import com.campusquest.domain.validation.GamePublishValidator
import com.campusquest.ui.auth.AuthViewModel
import com.campusquest.ui.creator.CreateGameViewModel
import com.campusquest.ui.creator.CreatorDashboardViewModel
import com.campusquest.ui.leaderboard.LeaderboardViewModel
import com.campusquest.ui.map.CampusMapViewModel
import com.campusquest.ui.player.GameDetailViewModel
import com.campusquest.ui.player.GamesListViewModel
import com.campusquest.ui.quest.QuestScanViewModel

class ViewModelFactory(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(GamesListViewModel::class.java) -> {
                GamesListViewModel(gameRepository) as T
            }
            modelClass.isAssignableFrom(GameDetailViewModel::class.java) -> {
                GameDetailViewModel(gameRepository) as T
            }
            modelClass.isAssignableFrom(CampusMapViewModel::class.java) -> {
                CampusMapViewModel(gameRepository = gameRepository) as T
            }
            modelClass.isAssignableFrom(QuestScanViewModel::class.java) -> {
                QuestScanViewModel(gameRepository = gameRepository) as T
            }
            modelClass.isAssignableFrom(LeaderboardViewModel::class.java) -> {
                LeaderboardViewModel(gameRepository) as T
            }
            modelClass.isAssignableFrom(CreatorDashboardViewModel::class.java) -> {
                CreatorDashboardViewModel(gameRepository) as T
            }
            modelClass.isAssignableFrom(CreateGameViewModel::class.java) -> {
                CreateGameViewModel(
                    gameRepository = gameRepository,
                    validator = GamePublishValidator()
                ) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        fun from(fragment: Fragment): ViewModelFactory {
            val app = fragment.requireActivity().application as CampusQuestApplication
            return ViewModelFactory(app.gameRepository, app.authRepository)
        }
    }
}
