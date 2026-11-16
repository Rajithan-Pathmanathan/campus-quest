package com.campusquest.ui.creator.validation

import com.campusquest.ui.creator.CheckpointEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckpointEditorViewModelTest {

    private val repo = FakeGameRepository()

    @Before
    fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun filledVm(gameId: String = "G1"): CheckpointEditorViewModel {
        val vm = CheckpointEditorViewModel(repo, gameId, null)
        vm.update {
            it.copy(name = "Old Library", lat = "6.9", lng = "79.8", radiusM = "20",
                minLux = "100", maxLux = "350", clue = "A clue", lore = "Some lore")
        }
        return vm
    }

    @Test
    fun emptyForm_showsErrorsAndDoesNotSave() {
        val vm = CheckpointEditorViewModel(repo, "G1", null)
        vm.save()
        assertTrue(vm.state.value.errors.containsKey("name"))
        assertTrue(vm.state.value.errors.containsKey("lat"))
        assertEquals(0, repo.checkpoints.size)
    }

    @Test
    fun minLuxAboveMax_isRejected() {
        val vm = filledVm()
        vm.update { it.copy(minLux = "500", maxLux = "100") }
        vm.save()
        assertTrue(vm.state.value.errors.containsKey("lux"))
        assertEquals(0, repo.checkpoints.size)
    }

    @Test
    fun validForm_savesCheckpointWithCorrectGameId() {
        val vm = filledVm("GAME-A")
        vm.save()
        assertEquals(1, repo.checkpoints.size)
        assertEquals("GAME-A", repo.checkpoints[0].gameId)
        assertEquals(20f, repo.checkpoints[0].radiusM, 0.001f)
        assertEquals("TILT_MAZE", repo.checkpoints[0].motionType)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun editing_keepsSameIdAndDoesNotCreateDuplicate() {
        val vm = filledVm()
        vm.save()
        val id = repo.checkpoints[0].id

        val editVm = CheckpointEditorViewModel(repo, "G1", id)
        editVm.update { it.copy(clue = "New clue", radiusM = "40") }
        editVm.save()

        assertEquals(1, repo.checkpoints.size)
        assertEquals(id, repo.checkpoints[0].id)
        assertEquals("New clue", repo.checkpoints[0].clue)
        assertEquals(40f, repo.checkpoints[0].radiusM, 0.001f)
    }

    @Test
    fun savingOneGame_doesNotTouchAnotherGame() {
        val a = filledVm("GAME-A")
        a.save()
        val b = filledVm("GAME-B")
        b.save()
        assertEquals(1, repo.checkpoints.count { it.gameId == "GAME-A" })
        assertEquals(1, repo.checkpoints.count { it.gameId == "GAME-B" })
    }
}
