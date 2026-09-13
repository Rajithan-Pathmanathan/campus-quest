package com.campusquest

import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.ui.creator.CreatorDashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreatorDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CreatorDashboardViewModel

    private val sampleGames = listOf(
        Game(
            id = "G_PUB_1",
            title = "Colombo Heritage",
            description = "Historic trail",
            creatorId = "C1",
            creatorName = "Alan Turing",
            status = GameStatus.PUBLISHED,
            checkpointCount = 3
        ),
        Game(
            id = "G_DRAFT_1",
            title = "Quantum Physics",
            description = "Lab exploration",
            creatorId = "C1",
            creatorName = "Alan Turing",
            status = GameStatus.DRAFT,
            checkpointCount = 2
        ),
        Game(
            id = "G_DRAFT_2",
            title = "Botanical Garden",
            description = "Flora trail",
            creatorId = "C1",
            creatorName = "Alan Turing",
            status = GameStatus.DRAFT,
            checkpointCount = 4
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CreatorDashboardViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAuthoredGames_partitionsDraftsAndPublishedQuestsCorrectly() {
        viewModel.loadAuthoredGames("C1", "Alan Turing", sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.totalQuests)
        assertEquals(1, state.publishedGames.size)
        assertEquals(2, state.draftGames.size)
        assertEquals(9, state.totalCheckpointsCreated) // 3 + 2 + 4

        assertEquals("Colombo Heritage", state.publishedGames[0].title)
        assertEquals("Quantum Physics", state.draftGames[0].title)
        assertEquals("Botanical Garden", state.draftGames[1].title)
    }

    @Test
    fun deleteGame_removesGameAndUpdatesMetrics() {
        viewModel.loadAuthoredGames("C1", "Alan Turing", sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteGame("G_DRAFT_1")

        val state = viewModel.uiState.value
        assertEquals(2, state.totalQuests)
        assertEquals(1, state.draftGames.size)
        assertEquals("Botanical Garden", state.draftGames[0].title)
        assertEquals(7, state.totalCheckpointsCreated) // 3 + 4
    }

    @Test
    fun publishGame_movesDraftToPublishedList() {
        viewModel.loadAuthoredGames("C1", "Alan Turing", sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.publishGame("G_DRAFT_1")

        val state = viewModel.uiState.value
        assertEquals(3, state.totalQuests)
        assertEquals(1, state.draftGames.size)
        assertEquals(2, state.publishedGames.size)
        assertEquals("Quantum Physics", state.publishedGames[0].title)
        assertEquals(GameStatus.PUBLISHED, state.publishedGames[0].status)
    }

    @Test
    fun loadAuthoredGames_emptyList_setsZeroMetricsAndEmptyLists() {
        viewModel.loadAuthoredGames("C_EMPTY", "New Creator", emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0, state.totalQuests)
        assertEquals(0, state.totalCheckpointsCreated)
        assertTrue(state.draftGames.isEmpty())
        assertTrue(state.publishedGames.isEmpty())
    }
}
