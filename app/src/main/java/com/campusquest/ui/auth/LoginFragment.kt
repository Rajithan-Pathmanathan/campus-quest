package com.campusquest.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentLoginBinding
import com.campusquest.domain.model.AuthUser
import com.campusquest.ui.common.UiState
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            if (viewModel.isRegisterMode.value) {
                viewModel.register(
                    email = email,
                    password = password,
                    displayName = binding.etDisplayName.text?.toString().orEmpty()
                )
            } else {
                viewModel.login(email, password)
            }
        }
        binding.btnGuestLogin.setOnClickListener { viewModel.continueAsGuest() }
        binding.btnToggleMode.setOnClickListener { viewModel.toggleMode() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { renderState(it) } }
                launch { viewModel.isRegisterMode.collect { renderMode(it) } }
            }
        }
    }

    private fun renderMode(register: Boolean) {
        binding.tilDisplayName.isVisible = register
        binding.btnGuestLogin.isVisible = !register
        binding.btnLogin.text = getString(
            if (register) R.string.login_create_account_button else R.string.login_button
        )
        binding.btnToggleMode.text = getString(
            if (register) R.string.login_toggle_to_login else R.string.login_toggle_to_register
        )
    }

    private fun renderState(state: UiState<AuthUser>) {
        val loading = state is UiState.Loading
        binding.progressBar.isVisible = loading
        binding.btnLogin.isEnabled = !loading
        binding.btnGuestLogin.isEnabled = !loading
        binding.btnToggleMode.isEnabled = !loading

        val error = (state as? UiState.Error)?.message
        binding.tvError.isVisible = error != null
        binding.tvError.text = error

        if (state is UiState.Success &&
            findNavController().currentDestination?.id == R.id.loginFragment
        ) {
            findNavController().navigate(R.id.action_login_to_playerDashboard)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
