package com.campusquest.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Quest Details view, checkpoint sequence previews, and the player join session.
 */
class GameDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameDetailUiState(isLoading = true))
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    fun loadGameDetails(
        gameId: String,
        customGame: Game? = null,
        customCheckpoints: List<Checkpoint>? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val game = customGame ?: getSampleGame(gameId)
            val checkpoints = (customCheckpoints ?: getSampleCheckpoints(gameId)).sortedBy { it.order }

            if (game != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        game = game,
                        checkpoints = checkpoints,
                        isJoined = false,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        game = null,
                        checkpoints = emptyList(),
                        errorMessage = "Quest with ID '$gameId' not found."
                    )
                }
            }
        }
    }

    fun joinQuest() {
        _uiState.update { it.copy(isJoined = true) }
    }

    private fun getSampleGame(gameId: String): Game? {
        return when (gameId) {
            "GAME_HERITAGE_001" -> Game(
                id = "GAME_HERITAGE_001",
                title = "Colombo Heritage Trail",
                description = "Explore iconic colonial architecture, historical landmarks, and hidden relics across the heart of Colombo.",
                creatorId = "CREATOR_PROF_ALAN",
                creatorName = "Prof. Alan Turing",
                status = GameStatus.PUBLISHED,
                checkpointCount = 3,
                publishedAt = System.currentTimeMillis() - 86400000L
            )
            "GAME_CAMPUS_INNOVATION_002" -> Game(
                id = "GAME_CAMPUS_INNOVATION_002",
                title = "Engineering Quad Cipher",
                description = "Decrypt sensor secrets hidden in labs, robotics arenas, and innovation hubs.",
                creatorId = "CREATOR_DR_GRACE",
                creatorName = "Dr. Grace Hopper",
                status = GameStatus.PUBLISHED,
                checkpointCount = 2,
                publishedAt = System.currentTimeMillis() - 43200000L
            )
            else -> Game(
                id = gameId,
                title = "Campus Adventure: $gameId",
                description = "A dynamically generated exploration quest across campus checkpoints.",
                creatorId = "CREATOR_DYNAMIC",
                creatorName = "Campus Quest Creator",
                status = GameStatus.PUBLISHED,
                checkpointCount = 2,
                publishedAt = System.currentTimeMillis()
            )
        }
    }

    private fun getSampleCheckpoints(gameId: String): List<Checkpoint> {
        return listOf(
            Checkpoint(
                id = "CP_BELL_TOWER",
                gameId = gameId,
                name = "Historic Clock Tower",
                lat = 6.9360,
                lng = 79.8436,
                radiusM = 25f,
                lightSignature = LightSignature(200f, 700f),
                clue = "Look skyward where the timekeeper chimes above the quad.",
                lore = "Erected in 1857 as a lighthouse and clock tower.",
                order = 1,
                motionType = "SWEEP"
            ),
            Checkpoint(
                id = "CP_OLD_PARLIAMENT",
                gameId = gameId,
                name = "Old Parliament Building",
                lat = 6.9328,
                lng = 79.8436,
                radiusM = 30f,
                lightSignature = LightSignature(150f, 600f),
                clue = "Where statesmen gathered beside the Indian Ocean breeze.",
                lore = "Neo-baroque architecture constructed in 1930.",
                order = 2,
                motionType = "SHAKE"
            ),
            Checkpoint(
                id = "CP_GALLE_FACE",
                gameId = gameId,
                name = "Galle Face Promenade",
                lat = 6.9248,
                lng = 79.8427,
                radiusM = 35f,
                lightSignature = LightSignature(300f, 1000f),
                clue = "Feel the coastal winds and sunset light resonance.",
                lore = "Originally laid out in 1859 by Governor Henry Ward.",
                order = 3,
                motionType = "TILT"
            )
        )
    }
}
