package com.campusquest.ui.creator.validation

import com.campusquest.domain.model.GameStatus
import com.campusquest.ui.creator.CreateGameViewModel
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
class CreateGameViewModelTest {

    private val repo = FakeGameRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(userId: String? = "me") =
        CreateGameViewModel(repo, { userId }, { "Manumi" })

    @Test
    fun blankTitle_showsErrorAndDoesNotSave() {
        val vm = vm()
        vm.saveDraft()
        assertNotNull(vm.state.value.titleError)
        assertEquals(0, repo.games.size)
    }

    @Test
    fun validInput_savesDraftWithCreatorFromAuth() {
        val vm = vm("user-123")
        vm.onTitleChanged("Heritage Trail")
        vm.onDescriptionChanged("A walk")
        vm.saveDraft()
        assertEquals(1, repo.games.size)
        assertEquals("user-123", repo.games[0].creatorId)
        assertEquals(GameStatus.DRAFT, repo.games[0].status)
        assertNotNull(vm.state.value.savedGameId)
    }

    @Test
    fun emptyDescription_isAllowedForDraft() {
        val vm = vm()
        vm.onTitleChanged("Title")
        vm.saveDraft()
        assertNull(vm.state.value.descriptionError)
        assertEquals(1, repo.games.size)
    }

    @Test
    fun notSignedIn_setsSaveError() {
        val vm = vm(null)
        vm.onTitleChanged("Title")
        vm.saveDraft()
        assertNotNull(vm.state.value.saveError)
        assertEquals(0, repo.games.size)
    }

}