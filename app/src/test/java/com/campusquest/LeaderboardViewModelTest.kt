package com.campusquest

import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.ui.leaderboard.LeaderboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LeaderboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LeaderboardViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadLeaderboard_sortsByScoreDescending_andBreaksTiesByCompletionTimeAscending() {
        val rawEntries = listOf(
            GameLeaderboardEntry("u1", "Slow_High", 500, 1200, 0),
            GameLeaderboardEntry("u2", "Fast_High", 500, 800, 0),
            GameLeaderboardEntry("u3", "Top_Scorer", 700, 950, 0),
            GameLeaderboardEntry("u4", "Low_Scorer", 200, 500, 0)
        )

        viewModel.loadLeaderboard("GAME_001", "Custom Quest", rawEntries)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(4, state.totalEntries)

        // Rank 1: Top_Scorer (700 pts)
        assertEquals("Top_Scorer", state.firstPlace?.userName)
        assertEquals(1, state.firstPlace?.rank)

        // Rank 2: Fast_High (500 pts, 800s)
        assertEquals("Fast_High", state.secondPlace?.userName)
        assertEquals(2, state.secondPlace?.rank)

        // Rank 3: Slow_High (500 pts, 1200s)
        assertEquals("Slow_High", state.thirdPlace?.userName)
        assertEquals(3, state.thirdPlace?.rank)

        // Rank 4: Low_Scorer (200 pts)
        assertEquals(1, state.rankedEntries.size)
        assertEquals("Low_Scorer", state.rankedEntries[0].userName)
        assertEquals(4, state.rankedEntries[0].rank)
    }

    @Test
    fun loadLeaderboard_withDefaultSampleData_populatesPodiumAndRankedEntries() {
        viewModel.loadLeaderboard("GAME_HERITAGE_001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Colombo Heritage Trail", state.gameTitle)
        assertEquals(3, state.podiumEntries.size)
        assertEquals(3, state.rankedEntries.size)
        assertEquals(6, state.totalEntries)

        assertEquals("CyberSeeker_99", state.firstPlace?.userName)
        assertEquals(650, state.firstPlace?.score)
    }

    @Test
    fun loadLeaderboard_identifiesCurrentPlayerPosition() {
        viewModel.loadLeaderboard(
            gameId = "GAME_HERITAGE_001",
            currentUserId = "u_current_player"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val player = state.currentPlayerEntry
        assertNotNull(player)
        assertEquals("Player_You", player?.userName)
        assertEquals(4, player?.rank)
        assertEquals(420, player?.score)
    }

    @Test
    fun loadLeaderboard_emptyList_setsEmptyStateWithoutError() {
        viewModel.loadLeaderboard(
            gameId = "GAME_EMPTY",
            customTitle = "Empty Quest",
            customEntries = emptyList()
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0, state.totalEntries)
        assertTrue(state.podiumEntries.isEmpty())
        assertTrue(state.rankedEntries.isEmpty())
        assertNull(state.currentPlayerEntry)
        assertNull(state.errorMessage)
    }
}
