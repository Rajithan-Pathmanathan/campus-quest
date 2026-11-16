package com.campusquest.ui.creator.validation

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.CheckpointActivity
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.LightSignature
import com.campusquest.ui.creator.PublishUiState
import com.campusquest.ui.creator.PublishViewModel
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
class PublishViewModelTest {

    private val repo = FakeGameRepository()

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun addGame() {
        repo.games += Game(id = "G1", title = "Trail", description = "A walk", creatorId = "me", creatorName = "Me")
    }

    private fun addCheckpoint(clue: String = "clue") {
        repo.checkpoints += (1..3).map { order ->
            Checkpoint(
                id = "c$order", gameId = "G1", name = "Checkpoint $order", lat = 6.9, lng = 79.8,
                lightSignature = LightSignature(0f, 100f), clue = if (order == 1) clue else "clue",
                lore = "lore", order = order, motionType = CheckpointActivity.forOrder(order).name
            )
        }
    }

    @Test fun gameWithNoCheckpoints_isInvalid_andNotPublished() {
        addGame()
        val vm = PublishViewModel(repo)
        vm.requestPublish("G1")
        assertTrue(vm.state.value is PublishUiState.Invalid)
        vm.confirmPublish()
        assertEquals(0, repo.publishCalls)
    }

    @Test fun missingClue_isReportedWithCheckpointNumber() {
        addGame(); addCheckpoint(clue = "")
        val vm = PublishViewModel(repo)
        vm.requestPublish("G1")
        val s = vm.state.value as PublishUiState.Invalid
        assertTrue(s.problems.any { it.contains("Checkpoint 1") })
    }

    @Test fun validGame_publishesExactlyOnce_evenIfTappedTwice() {
        addGame(); addCheckpoint()
        val vm = PublishViewModel(repo)
        vm.requestPublish("G1")
        assertTrue(vm.state.value is PublishUiState.ReadyToConfirm)
        vm.confirmPublish()
        vm.confirmPublish()
        assertEquals(1, repo.publishCalls)
        assertTrue(vm.state.value is PublishUiState.Success)
    }

    @Test fun confirmWithoutValidation_doesNothing() {
        addGame(); addCheckpoint()
        val vm = PublishViewModel(repo)
        vm.confirmPublish()
        assertEquals(0, repo.publishCalls)
    }
}
