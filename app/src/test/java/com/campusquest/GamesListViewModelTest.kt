package com.campusquest

import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.ui.player.GamesListViewModel
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
class GamesListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: GamesListViewModel

    private val sampleGames = listOf(
        Game(
            id = "G1",
            title = "Colombo Heritage",
            description = "Colonial relics",
            creatorId = "C1",
            creatorName = "Alan Turing",
            status = GameStatus.PUBLISHED,
            checkpointCount = 5
        ),
        Game(
            id = "G2",
            title = "Engineering Quad",
            description = "Robotics and sensors",
            creatorId = "C2",
            creatorName = "Grace Hopper",
            status = GameStatus.PUBLISHED,
            checkpointCount = 4
        ),
        Game(
            id = "G3",
            title = "Secret Lab Draft",
            description = "Draft under construction",
            creatorId = "C3",
            creatorName = "Ada Lovelace",
            status = GameStatus.DRAFT,
            checkpointCount = 3
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = GamesListViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadGames_populatesAllAndFilteredGames() {
        viewModel.loadGames(sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.allGames.size)
        assertEquals(3, state.filteredGames.size)
        assertFalse(state.isEmpty)
    }

    @Test
    fun onSearchQueryChanged_filtersMatchingTitles() {
        viewModel.loadGames(sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Heritage")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredGames.size)
        assertEquals("Colombo Heritage", state.filteredGames[0].title)
    }

    @Test
    fun onSearchQueryChanged_filtersMatchingCreators() {
        viewModel.loadGames(sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Grace")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredGames.size)
        assertEquals("Engineering Quad", state.filteredGames[0].title)
    }

    @Test
    fun onStatusFilterChanged_filtersOnlyPublishedGames() {
        viewModel.loadGames(sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onStatusFilterChanged(GameStatus.PUBLISHED)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.filteredGames.size)
        assertTrue(state.filteredGames.all { it.status == GameStatus.PUBLISHED })
    }

    @Test
    fun onStatusFilterChanged_filtersOnlyDraftGames() {
        viewModel.loadGames(sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onStatusFilterChanged(GameStatus.DRAFT)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredGames.size)
        assertEquals("Secret Lab Draft", state.filteredGames[0].title)
    }

    @Test
    fun searchWithNoMatch_triggersEmptyState() {
        viewModel.loadGames(sampleGames)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("NonExistentQueryXYZ")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.filteredGames.size)
        assertTrue("Empty state should be active", state.isEmpty)
    }
}
