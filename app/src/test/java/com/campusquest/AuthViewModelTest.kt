package com.campusquest

import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.ui.auth.AuthUiState
import com.campusquest.ui.auth.AuthViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val currentUserFlow = MutableStateFlow<AuthUser?>(null)
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { authRepository.currentUserState } returns currentUserFlow
        viewModel = AuthViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signInWithEmail_emptyFields_setsErrorState() {
        viewModel.signInWithEmail("", "")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Email and password cannot be empty.", (state as AuthUiState.Error).message)
    }

    @Test
    fun signInWithEmail_success_updatesToAuthenticated() {
        val testUser = AuthUser("uid123", "Explorer Test", "test@campusquest.com")
        coEvery { authRepository.signInWithEmail("test@campusquest.com", "pass123") } returns Result.success(testUser)

        viewModel.signInWithEmail("test@campusquest.com", "pass123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Authenticated)
        assertEquals(testUser, (state as AuthUiState.Authenticated).user)
    }

    @Test
    fun signInWithEmail_failure_updatesToError() {
        coEvery { authRepository.signInWithEmail(any(), any()) } returns Result.failure(Exception("Invalid credentials"))

        viewModel.signInWithEmail("bad@campusquest.com", "wrongpass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Invalid credentials", (state as AuthUiState.Error).message)
    }

    @Test
    fun signUpWithEmail_success_updatesToAuthenticated() {
        val newUser = AuthUser("uid999", "New Cadet", "cadet@campusquest.com")
        coEvery {
            authRepository.signUpWithEmail("cadet@campusquest.com", "pass1234", "New Cadet")
        } returns Result.success(newUser)

        viewModel.signUpWithEmail("cadet@campusquest.com", "pass1234", "New Cadet")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Authenticated)
        assertEquals("New Cadet", (state as AuthUiState.Authenticated).user.displayName)
    }

    @Test
    fun signInAnonymously_success_updatesToAuthenticatedGuest() {
        val guestUser = AuthUser("guest_101", "Guest Explorer", null, isAnonymous = true)
        coEvery { authRepository.signInAnonymously() } returns Result.success(guestUser)

        viewModel.signInAnonymously()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Authenticated)
        assertTrue((state as AuthUiState.Authenticated).user.isAnonymous)
    }

    @Test
    fun signOut_callsRepositoryAndResetsToIdle() {
        viewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.signOut() }
        assertTrue(viewModel.uiState.value is AuthUiState.Idle)
    }

    @Test
    fun clearError_resetsErrorToIdle() {
        viewModel.signInWithEmail("", "")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AuthUiState.Error)

        viewModel.clearError()
        assertTrue(viewModel.uiState.value is AuthUiState.Idle)
    }
}
