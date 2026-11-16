package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.databinding.FragmentPlayerDashboardBinding
import com.campusquest.ui.common.AppProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PlayerDashboardFragment : Fragment() {

    private var _binding: FragmentPlayerDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerDashboardBinding.inflate(inflater, container, false)
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

        val uid = auth.getCurrentUserId()
        if (uid != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    CampusQuestApplication.instance.database.playerProgressDao().observeUserProgress(uid).collect { rows ->
                        val score = rows.sumOf { it.score }
                        val relics = rows.sumOf { it.completedCheckpointIds.size }
                        binding.tvStats.text = getString(R.string.player_progress_stats, score, relics)
                    }
                }
            }
        }

        binding.btnBrowseQuests.setOnClickListener {
            findNavController().navigate(R.id.action_player_to_gamesList)
        }
        binding.btnSwitchCreator.setOnClickListener {
            findNavController().navigate(R.id.action_player_to_creatorDashboard)
        }
        binding.btnSignOut.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                auth.signOut()
                findNavController().navigate(R.id.action_player_to_login)
            }
        }
        binding.btnPrivacyPolicy.setOnClickListener { findNavController().navigate(R.id.action_global_privacyPolicy) }
        binding.btnDeleteAccount.setOnClickListener { confirmDeleteAccount() }
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_delete_title)
            .setMessage(R.string.account_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.account_delete_confirm) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppProviders.authRepository.deleteAccount()
                    if (_binding == null) return@launch
                    result.onSuccess {
                        Toast.makeText(requireContext(), R.string.account_deleted, Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_player_to_login)
                    }.onFailure { e ->
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
