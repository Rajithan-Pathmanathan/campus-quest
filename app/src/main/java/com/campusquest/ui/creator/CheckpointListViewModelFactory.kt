package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusquest.domain.repository.GameRepository

class CheckpointListViewModelFactory(
    private val repository: GameRepository,
    private val gameId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CheckpointListViewModel(repository, gameId) as T
}