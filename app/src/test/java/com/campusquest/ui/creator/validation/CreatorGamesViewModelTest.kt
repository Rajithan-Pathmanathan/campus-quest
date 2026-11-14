package com.campusquest.ui.creator.validation

import com.campusquest.ui.creator.CreatorGamesUiState
import com.campusquest.ui.creator.CreatorGamesViewModel
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreatorGamesViewModelTest {

    private val repo = FakeGameRepository()

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun game(id: String, creatorId: String, status: GameStatus = GameStatus.DRAFT) = Game(
        id = id, title = "Game $id", description = "d",
        creatorId = creatorId, creatorName = "Name", status = status
    )

    @Test fun showsOnlyMyGames() {
        repo.games += game("A", "me")
        repo.games += game("B", "someoneElse")
        val vm = CreatorGamesViewModel(repo) { "me" }
        assertEquals(listOf("A"), vm.state.value.games.map { it.id })
    }

    @Test fun splitsDraftsAndPublished() {
        repo.games += game("A", "me", GameStatus.DRAFT)
        repo.games += game("B", "me", GameStatus.PUBLISHED)
        val vm = CreatorGamesViewModel(repo) { "me" }
        assertEquals(1, vm.state.value.drafts.size)
        assertEquals(1, vm.state.value.published.size)
    }

    @Test fun zeroGames_isEmptyNotError() {
        val vm = CreatorGamesViewModel(repo) { "me" }
        assertEquals(emptyList<Game>(), vm.state.value.games)
        assertNull(vm.state.value.error)
    }

    @Test fun repositoryFailure_setsFriendlyError() {
        repo.failLoading = true
        val vm = CreatorGamesViewModel(repo) { "me" }
        assertNotNull(vm.state.value.error)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test fun notSignedIn_setsError() {
        val vm = CreatorGamesViewModel(repo) { null }
        assertNotNull(vm.state.value.error)
    }
}