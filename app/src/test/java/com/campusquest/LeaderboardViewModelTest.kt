package com.campusquest

import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.ui.leaderboard.LeaderboardViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LeaderboardViewModelTest {

    @get:Rule
    val mainRule = M1MainDispatcherRule()

    @Test
    fun `entries are sorted by score then completion time`() {
        val repo = M1FakeGameRepository()
        repo.leaderboardFlow = flowOf(
            listOf(
                m1Entry("A", score = 300, timeSeconds = 900),
                m1Entry("B", score = 500, timeSeconds = 1200),
                m1Entry("C", score = 500, timeSeconds = 800),
                m1Entry("D", score = 300, timeSeconds = 700)
            )
        )
        val vm = LeaderboardViewModel(repo)

        vm.load("g1")

        assertEquals(listOf("C", "B", "D", "A"), vm.uiState.value.entries.map { it.userId })
    }

    @Test
    fun `leaderboard is requested for the given game only`() {
        val repo = M1FakeGameRepository()
        val vm = LeaderboardViewModel(repo)

        vm.load("game-42")

        assertEquals(listOf("game-42"), repo.leaderboardRequests)
    }

    @Test
    fun `shows loading until the first entries arrive`() {
        val repo = M1FakeGameRepository()
        val source = MutableSharedFlow<List<GameLeaderboardEntry>>(replay = 1)
        repo.leaderboardFlow = source
        val vm = LeaderboardViewModel(repo)

        vm.load("g1")
        assertTrue(vm.uiState.value.isLoading)

        source.tryEmit(listOf(m1Entry("A", score = 100, timeSeconds = 60)))
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(1, vm.uiState.value.entries.size)
    }

    @Test
    fun `no entries gives the empty state`() {
        val repo = M1FakeGameRepository()
        repo.leaderboardFlow = flowOf(emptyList())
        val vm = LeaderboardViewModel(repo)

        vm.load("g1")

        assertTrue(vm.uiState.value.isEmpty)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `flow failure gives the error state`() {
        val repo = M1FakeGameRepository()
        repo.leaderboardFlow = flow { throw RuntimeException("Offline") }
        val vm = LeaderboardViewModel(repo)

        vm.load("g1")

        assertEquals("Offline", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `flow failure without a message shows a default error`() {
        val repo = M1FakeGameRepository()
        repo.leaderboardFlow = flow { throw RuntimeException() }
        val vm = LeaderboardViewModel(repo)

        vm.load("g1")

        assertEquals("Could not load leaderboard", vm.uiState.value.error)
    }
}
