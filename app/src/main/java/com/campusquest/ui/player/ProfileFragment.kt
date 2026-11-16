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
import com.campusquest.databinding.FragmentProfileBinding
import com.campusquest.ui.common.AppProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val auth = AppProviders.authRepository
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.currentUserState.collect { user ->
                    binding.tvProfileName.text = user?.displayName?.takeIf(String::isNotBlank)
                        ?: getString(R.string.profile_explorer)
                    binding.tvProfileEmail.text = user?.email
                        ?: getString(R.string.profile_guest_account)
                }
            }
        }
        auth.getCurrentUserId()?.let { userId ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    CampusQuestApplication.instance.database.playerProgressDao()
                        .observeUserProgress(userId)
                        .collect { progress -> binding.tvTotalPoints.text = progress.sumOf { it.score }.toString() }
                }
            }
        }

        binding.btnCreatorStudio.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_creatorDashboard)
        }
        binding.btnPrivacyPolicy.setOnClickListener {
            findNavController().navigate(R.id.action_global_privacyPolicy)
        }
        binding.btnSignOut.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                auth.signOut()
                findNavController().navigate(R.id.action_profile_to_login)
            }
        }
        binding.btnDeleteAccount.setOnClickListener { confirmDeleteAccount() }
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_delete_title)
            .setMessage(R.string.account_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.account_delete_confirm) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    AppProviders.authRepository.deleteAccount()
                        .onSuccess {
                            if (_binding == null) return@onSuccess
                            Toast.makeText(requireContext(), R.string.account_deleted, Toast.LENGTH_LONG).show()
                            findNavController().navigate(R.id.action_profile_to_login)
                        }
                        .onFailure { error ->
                            if (_binding != null) Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
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
