package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.repository.GameRepository
import com.campusquest.ui.creator.validation.GameValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GameEditorUiState(
    val gameId: String? = null,
    val title: String = "",
    val description: String = "",
    val titleError: String? = null,
    val descriptionError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedGameId: String? = null
)

class CreateGameViewModel(
    private val repository: GameRepository,
    private val currentUserId: () -> String?,
    private val currentUserName: () -> String?
) : ViewModel() {

    private val _state = MutableStateFlow(GameEditorUiState())
    val state: StateFlow<GameEditorUiState> = _state.asStateFlow()

    fun onTitleChanged(value: String) {
        _state.update { it.copy(title = value, titleError = null) }
    }

    fun onDescriptionChanged(value: String) {
        _state.update { it.copy(description = value, descriptionError = null) }
    }

    fun saveDraft() {
        val current = _state.value
        if (current.isSaving) return

        val titleError = GameValidator.validateTitle(current.title)
        val descError = GameValidator.validateDescription(current.description, forPublish = false)
        if (titleError != null || descError != null) {
            _state.update { it.copy(titleError = titleError, descriptionError = descError) }
            return
        }

        val creatorId = currentUserId()
        if (creatorId == null) {
            _state.update { it.copy(saveError = "Please sign in before creating a game.") }
            return
        }

        _state.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                val game = Game(
                    id = current.gameId ?: UUID.randomUUID().toString(),
                    title = current.title.trim(),
                    description = current.description.trim(),
                    creatorId = creatorId,
                    creatorName = currentUserName() ?: "Creator",
                    status = GameStatus.DRAFT
                )
                repository.createGame(game)
                    .onSuccess { saved ->
                        _state.update { it.copy(isSaving = false, gameId = saved.id, savedGameId = saved.id) }
                    }
                    .onFailure {
                        _state.update {
                            it.copy(isSaving = false, saveError = "Unable to save. Check your connection and try again.")
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, saveError = "Unable to save. Check your connection and try again.")
                }
            }
        }
    }

    fun onSavedHandled() {
        _state.update { it.copy(savedGameId = null) }
    }
}