package com.campusquest.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Game
import com.campusquest.domain.repository.GameRepository
import com.campusquest.ui.common.AppProviders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class JoinStatus { IDLE, JOINING, JOINED, FAILED }

data class GameDetailUiState(
    val isLoading: Boolean = true,
    val game: Game? = null,
    val error: String? = null,
    val joinStatus: JoinStatus = JoinStatus.IDLE,
    val joinError: String? = null
)

class GameDetailViewModel(
    private val repository: GameRepository = AppProviders.gameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private var gameId: String = ""

    fun load(id: String, force: Boolean = false) {
        if (!force && id == gameId && _uiState.value.game != null) return
        gameId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val game = if (id.isBlank()) null else repository.getGameDetails(id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        game = game,
                        error = if (game == null) "Quest not found" else null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Could not load quest") }
            }
        }
    }

    fun join() {
        if (_uiState.value.joinStatus == JoinStatus.JOINING) return
        viewModelScope.launch {
            _uiState.update { it.copy(joinStatus = JoinStatus.JOINING, joinError = null) }
            val result = try {
                repository.joinGame(gameId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(joinStatus = JoinStatus.JOINED)
                } else {
                    it.copy(
                        joinStatus = JoinStatus.FAILED,
                        joinError = result.exceptionOrNull()?.message ?: "Could not join this quest"
                    )
                }
            }
        }
    }
}