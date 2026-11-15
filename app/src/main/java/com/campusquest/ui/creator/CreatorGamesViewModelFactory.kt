package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusquest.domain.repository.GameRepository

class CreatorGamesViewModelFactory(
    private val repository: GameRepository,
    private val currentUserId: () -> String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CreatorGamesViewModel(repository, currentUserId) as T
}