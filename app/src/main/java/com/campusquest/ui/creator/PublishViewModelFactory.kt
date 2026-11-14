package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusquest.domain.repository.GameRepository

class PublishViewModelFactory(
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PublishViewModel(repository) as T
}