package com.campusquest

import com.campusquest.ui.auth.LoginViewModel
import com.campusquest.ui.common.UiState
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainRule = M1MainDispatcherRule()

    private fun LoginViewModel.errorMessage(): String? = (uiState.value as? UiState.Error)?.message

    @Test
    fun `empty email shows an error and does not call the repository`() {
        val repo = M1FakeAuthRepository()
        val vm = LoginViewModel(repo)

        vm.login("", "secret1")

        assertEquals("Please enter email and password", vm.errorMessage())
        assertEquals(0, repo.signInCalls)
    }

    @Test
    fun `empty password shows an error and does not call the repository`() {
        val repo = M1FakeAuthRepository()
        val vm = LoginViewModel(repo)

        vm.login("user@test.com", "")

        assertEquals("Please enter email and password", vm.errorMessage())
        assertEquals(0, repo.signInCalls)
    }

    @Test
    fun `successful login emits success and trims the email`() {
        val repo = M1FakeAuthRepository()
        val vm = LoginViewModel(repo)

        vm.login("  user@test.com ", "secret1")

        assertTrue(vm.uiState.value is UiState.Success)
        assertEquals("user@test.com", repo.lastEmail)
    }

    @Test
    fun `failed login shows the repository error message`() {
        val repo = M1FakeAuthRepository()
        repo.signInResult = Result.failure(IllegalArgumentException("Wrong password"))
        val vm = LoginViewModel(repo)

        vm.login("user@test.com", "secret1")

        assertEquals("Wrong password", vm.errorMessage())
    }

    @Test
    fun `failed login without a message shows a default error`() {
        val repo = M1FakeAuthRepository()
        repo.signInResult = Result.failure(RuntimeException())
        val vm = LoginViewModel(repo)

        vm.login("user@test.com", "secret1")

        assertEquals("Sign-in failed", vm.errorMessage())
    }

    @Test
    fun `login shows loading while the request is pending`() {
        val repo = M1FakeAuthRepository()
        val gate = CompletableDeferred<Unit>()
        repo.signInGate = gate
        val vm = LoginViewModel(repo)

        vm.login("user@test.com", "secret1")
        assertTrue(vm.uiState.value is UiState.Loading)

        gate.complete(Unit)
        assertTrue(vm.uiState.value is UiState.Success)
    }

    @Test
    fun `a second tap while loading is ignored`() {
        val repo = M1FakeAuthRepository()
        repo.signInGate = CompletableDeferred()
        val vm = LoginViewModel(repo)

        vm.login("user@test.com", "secret1")
        vm.login("user@test.com", "secret1")

        assertEquals(1, repo.signInCalls)
    }

    @Test
    fun `guest login emits success for an anonymous user`() {
        val repo = M1FakeAuthRepository()
        val vm = LoginViewModel(repo)

        vm.continueAsGuest()

        val state = vm.uiState.value
        assertTrue(state is UiState.Success)
        assertTrue((state as UiState.Success).data.isAnonymous)
    }
}
