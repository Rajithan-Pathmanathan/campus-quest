package com.campusquest

import com.campusquest.ui.player.GameDetailViewModel
import com.campusquest.ui.player.JoinStatus
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class GameDetailViewModelTest {

    @get:Rule
    val mainRule = M1MainDispatcherRule()

    @Test
    fun `loads the game details`() {
        val repo = M1FakeGameRepository()
        repo.games = listOf(m1Game("g1", title = "Heritage"))
        val vm = GameDetailViewModel(repo)

        vm.load("g1")

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Heritage", state.game?.title)
    }

    @Test
    fun `unknown game id shows quest not found`() {
        val repo = M1FakeGameRepository()
        val vm = GameDetailViewModel(repo)

        vm.load("missing")

        val state = vm.uiState.value
        assertNull(state.game)
        assertEquals("Quest not found", state.error)
    }

    @Test
    fun `blank game id shows quest not found`() {
        val vm = GameDetailViewModel(M1FakeGameRepository())

        vm.load("")

        assertEquals("Quest not found", vm.uiState.value.error)
    }

    @Test
    fun `repository exception shows the error`() {
        val repo = M1FakeGameRepository()
        repo.detailsError = RuntimeException("boom")
        val vm = GameDetailViewModel(repo)

        vm.load("g1")

        assertEquals("boom", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `join success marks the quest as joined`() {
        val repo = M1FakeGameRepository()
        repo.games = listOf(m1Game("g1"))
        val vm = GameDetailViewModel(repo)
        vm.load("g1")

        vm.join()

        assertEquals(JoinStatus.JOINED, vm.uiState.value.joinStatus)
        assertEquals(listOf("g1"), repo.joinCalls)
    }

    @Test
    fun `join failure shows the error and allows a retry`() {
        val repo = M1FakeGameRepository()
        repo.games = listOf(m1Game("g1"))
        repo.joinResult = Result.failure(IllegalStateException("Not allowed"))
        val vm = GameDetailViewModel(repo)
        vm.load("g1")

        vm.join()
        assertEquals(JoinStatus.FAILED, vm.uiState.value.joinStatus)
        assertEquals("Not allowed", vm.uiState.value.joinError)

        repo.joinResult = Result.success(Unit)
        vm.join()
        assertEquals(JoinStatus.JOINED, vm.uiState.value.joinStatus)
        assertNull(vm.uiState.value.joinError)
    }

    @Test
    fun `join failure without a message shows a default error`() {
        val repo = M1FakeGameRepository()
        repo.games = listOf(m1Game("g1"))
        repo.joinResult = Result.failure(RuntimeException())
        val vm = GameDetailViewModel(repo)
        vm.load("g1")

        vm.join()

        assertEquals("Could not join this quest", vm.uiState.value.joinError)
    }

    @Test
    fun `a second join tap while joining is ignored`() {
        val repo = M1FakeGameRepository()
        repo.games = listOf(m1Game("g1"))
        val gate = CompletableDeferred<Unit>()
        repo.joinGate = gate
        val vm = GameDetailViewModel(repo)
        vm.load("g1")

        vm.join()
        vm.join()

        assertEquals(JoinStatus.JOINING, vm.uiState.value.joinStatus)
        assertEquals(1, repo.joinCalls.size)

        gate.complete(Unit)
        assertEquals(JoinStatus.JOINED, vm.uiState.value.joinStatus)
    }
}
