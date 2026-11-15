package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusquest.domain.repository.GameRepository

class CreateGameViewModelFactory(
    private val repository: GameRepository,
    private val currentUserId: () -> String?,
    private val currentUserName: () -> String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CreateGameViewModel(repository, currentUserId, currentUserName) as T
}