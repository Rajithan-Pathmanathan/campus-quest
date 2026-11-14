package com.campusquest

import com.campusquest.ui.auth.LoginViewModel
import com.campusquest.ui.common.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginRegisterViewModelTest {

    @get:Rule
    val mainRule = M1MainDispatcherRule()

    private fun LoginViewModel.errorMessage(): String? = (uiState.value as? UiState.Error)?.message

    @Test
    fun `register with a blank name shows an error`() {
        val vm = LoginViewModel(M1FakeAuthRepository())

        vm.register("user@test.com", "secret1", "  ")

        assertEquals("Please enter your name", vm.errorMessage())
    }

    @Test
    fun `register with an invalid email shows an error`() {
        val vm = LoginViewModel(M1FakeAuthRepository())

        vm.register("not-an-email", "secret1", "Sobashi")

        assertEquals("Please enter a valid email address", vm.errorMessage())
    }

    @Test
    fun `register with a short password shows an error`() {
        val vm = LoginViewModel(M1FakeAuthRepository())

        vm.register("user@test.com", "123", "Sobashi")

        assertEquals("Password must be at least 6 characters", vm.errorMessage())
    }

    @Test
    fun `register with valid details emits success`() {
        val vm = LoginViewModel(M1FakeAuthRepository())

        vm.register("user@test.com", "secret1", "Sobashi")

        assertTrue(vm.uiState.value is UiState.Success)
    }

    @Test
    fun `register failure shows the repository message`() {
        val repo = M1FakeAuthRepository()
        repo.signInResult = Result.failure(IllegalStateException("Email already in use"))
        val vm = LoginViewModel(repo)

        vm.register("user@test.com", "secret1", "Sobashi")

        assertEquals("Email already in use", vm.errorMessage())
    }

    @Test
    fun `toggling the mode switches the form and clears the error`() {
        val vm = LoginViewModel(M1FakeAuthRepository())
        vm.login("", "")
        assertTrue(vm.uiState.value is UiState.Error)
        assertFalse(vm.isRegisterMode.value)

        vm.toggleMode()

        assertTrue(vm.isRegisterMode.value)
        assertTrue(vm.uiState.value is UiState.Idle)
    }
}
