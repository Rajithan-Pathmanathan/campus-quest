package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentHomeBinding
import com.campusquest.ui.common.AppProviders
import kotlinx.coroutines.launch

class PlayerDashboardFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = AppProviders.authRepository

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.currentUserState.collect { user ->
                    val name = user?.displayName?.takeIf { it.isNotBlank() }
                    binding.tvWelcome.text = if (name != null) getString(R.string.dashboard_welcome, name)
                    else getString(R.string.dashboard_welcome_default)
                }
            }
        }

        binding.cardCampus.setOnClickListener {
            findNavController().navigate(R.id.action_player_to_gamesList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
