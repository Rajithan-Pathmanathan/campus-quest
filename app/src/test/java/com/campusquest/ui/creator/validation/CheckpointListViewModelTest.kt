package com.campusquest.ui.creator.validation

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import com.campusquest.ui.creator.CheckpointListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckpointListViewModelTest {

    private val repo = FakeGameRepository()

    @Before
    fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun cp(id: String, gameId: String, order: Int) = Checkpoint(
        id = id, gameId = gameId, name = id, lat = 1.0, lng = 1.0,
        lightSignature = LightSignature(0f, 100f), clue = "c", lore = "l", order = order
    )

    @Test
    fun zeroCheckpoints_isEmptyNotError() {
        val vm = CheckpointListViewModel(repo, "G1")
        vm.load()
        assertEquals(0, vm.state.value.checkpoints.size)
        assertEquals(null, vm.state.value.error)
    }

    @Test
    fun listIsSortedByOrder() {
        repo.checkpoints += cp("b", "G1", 2)
        repo.checkpoints += cp("a", "G1", 1)
        val vm = CheckpointListViewModel(repo, "G1")
        vm.load()
        assertEquals(listOf("a", "b"), vm.state.value.checkpoints.map { it.id })
    }

    @Test
    fun moveDown_swapsOrderOfOneGameOnly() {
        repo.checkpoints += cp("a", "G1", 1)
        repo.checkpoints += cp("b", "G1", 2)
        repo.checkpoints += cp("x", "G2", 1)
        val vm = CheckpointListViewModel(repo, "G1")
        vm.load()
        vm.moveDown("a")
        assertEquals(listOf("b", "a"), vm.state.value.checkpoints.map { it.id })
        assertEquals(1, repo.checkpoints.first { it.id == "x" }.order) // other game untouched
    }
}