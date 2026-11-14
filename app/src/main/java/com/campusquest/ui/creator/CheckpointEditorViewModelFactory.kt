package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusquest.domain.repository.GameRepository

class CheckpointEditorViewModelFactory(
    private val repository: GameRepository,
    private val gameId: String,
    private val checkpointId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CheckpointEditorViewModel(repository, gameId, checkpointId) as T
}