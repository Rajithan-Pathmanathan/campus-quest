package com.campusquest

import com.campusquest.domain.model.GameStatus
import com.campusquest.ui.player.GamesViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GamesViewModelTest {

    @get:Rule
    val mainRule = M1MainDispatcherRule()

    @Test
    fun `shows loading while games are being fetched`() {
        val repo = M1FakeGameRepository()
        val gate = CompletableDeferred<Unit>()
        repo.availableGamesGate = gate

        val vm = GamesViewModel(repo)
        assertTrue(vm.uiState.value.isLoading)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `loads games successfully`() {
        val repo = M1FakeGameRepository()
        repo.games = listOf(m1Game("a"), m1Game("b"))

        val vm = GamesViewModel(repo)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf("a", "b"), state.games.map { it.id })
        assertFalse(state.isEmpty)
    }

    @Test
    fun `only published games are shown`() {
        val repo = M1FakeGameRepository()
        repo.games = listOf(
            m1Game("published", status = GameStatus.PUBLISHED),
            m1Game("draft", status = GameStatus.DRAFT),
            m1Game("closed", status = GameStatus.CLOSED)
        )

        val vm = GamesViewModel(repo)

        assertEquals(listOf("published"), vm.uiState.value.games.map { it.id })
    }

    @Test
    fun `no games gives the empty state`() {
        val repo = M1FakeGameRepository()

        val vm = GamesViewModel(repo)

        assertTrue(vm.uiState.value.isEmpty)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `repository failure gives the error state`() {
        val repo = M1FakeGameRepository()
        repo.availableGamesError = RuntimeException("Network down")

        val vm = GamesViewModel(repo)

        val state = vm.uiState.value
        assertEquals("Network down", state.error)
        assertTrue(state.games.isEmpty())
        assertFalse(state.isEmpty)
    }

    @Test
    fun `failure without a message shows a default error`() {
        val repo = M1FakeGameRepository()
        repo.availableGamesError = RuntimeException()

        val vm = GamesViewModel(repo)

        assertEquals("Could not load quests", vm.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads the games`() {
        val repo = M1FakeGameRepository()
        repo.availableGamesError = RuntimeException("Network down")
        val vm = GamesViewModel(repo)
        assertEquals("Network down", vm.uiState.value.error)

        repo.availableGamesError = null
        repo.games = listOf(m1Game("a"))
        vm.loadGames()

        val state = vm.uiState.value
        assertNull(state.error)
        assertEquals(listOf("a"), state.games.map { it.id })
    }
}
