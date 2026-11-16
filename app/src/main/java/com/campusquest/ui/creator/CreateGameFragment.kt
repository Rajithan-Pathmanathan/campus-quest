package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.databinding.FragmentCreateGameBinding
import kotlinx.coroutines.launch
import com.campusquest.ui.common.AppProviders

class CreateGameFragment : Fragment() {

    private var _binding: FragmentCreateGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateGameViewModel by viewModels {
        CreateGameViewModelFactory(
            repository = AppProviders.gameRepository,
            currentUserId = { AppProviders.authRepository.getCurrentUserId() },
            currentUserName = { AppProviders.authRepository.currentUserState.value?.displayName }
        )
    }

    private val publishViewModel: PublishViewModel by viewModels {
        PublishViewModelFactory(CampusQuestApplication.instance.gameRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        // Opened from Creator Studio with an existing draft: load it so it can be finished and published.
        arguments?.getString("gameId")?.let { id ->
            binding.toolbar.setTitle(R.string.creator_edit_quest_title)
            viewModel.loadExisting(id)
        }

        binding.etTitle.doAfterTextChanged { viewModel.onTitleChanged(it?.toString().orEmpty()) }
        binding.etDesc.doAfterTextChanged { viewModel.onDescriptionChanged(it?.toString().orEmpty()) }

        binding.btnSaveDraft.setOnClickListener { viewModel.saveDraft() }

        binding.btnAddCheckpoint.setOnClickListener {
            val gameId = viewModel.state.value.gameId
            if (gameId == null) {
                Toast.makeText(requireContext(), "Save the draft first, then add checkpoints.", Toast.LENGTH_SHORT).show()
            } else {
                val bundle = Bundle().apply { putString("gameId", gameId) }
                findNavController().navigate(R.id.action_createGame_to_checkpointList, bundle)
            }
        }

        binding.btnPublish.setOnClickListener {
            val gameId = viewModel.state.value.gameId
            if (gameId == null) {
                Toast.makeText(requireContext(), "Save the draft first, then publish.", Toast.LENGTH_SHORT).show()
            } else {
                publishViewModel.requestPublish(gameId)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (binding.etTitle.text?.toString() != state.title) binding.etTitle.setText(state.title)
                    if (binding.etDesc.text?.toString() != state.description) binding.etDesc.setText(state.description)
                    binding.tilTitle.error = state.titleError
                    binding.tilDesc.error = state.descriptionError
                    binding.btnSaveDraft.isEnabled = !state.isSaving
                    binding.btnSaveDraft.text = if (state.isSaving) "Saving..." else getString(R.string.creator_save_draft)

                    state.saveError?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    }
                    if (state.savedGameId != null) {
                        Toast.makeText(requireContext(), "Draft saved", Toast.LENGTH_SHORT).show()
                        viewModel.onSavedHandled()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                publishViewModel.state.collect { s -> renderPublish(s) }
            }
        }
    }

    private fun renderPublish(s: PublishUiState) {
        val busy = s is PublishUiState.Checking || s is PublishUiState.Publishing
        binding.btnPublish.isEnabled = !busy
        binding.btnPublish.text = when (s) {
            is PublishUiState.Publishing -> "Publishing..."
            is PublishUiState.Checking -> "Checking..."
            else -> getString(R.string.creator_publish_quest)
        }

        when (s) {
            is PublishUiState.Invalid -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Cannot publish yet")
                    .setMessage(s.problems.joinToString("\n") { "• $it" })
                    .setPositiveButton("OK", null)
                    .setOnDismissListener { publishViewModel.dismiss() }
                    .show()
            }
            is PublishUiState.ReadyToConfirm -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Publish Game?")
                    .setMessage("Once published, players will be able to discover and join this game.")
                    .setNegativeButton("Cancel") { _, _ -> publishViewModel.dismiss() }
                    .setPositiveButton("Publish") { _, _ -> publishViewModel.confirmPublish() }
                    .setOnCancelListener { publishViewModel.dismiss() }
                    .show()
            }
            is PublishUiState.Success -> {
                Toast.makeText(requireContext(), "Game published. It is now available to players.", Toast.LENGTH_LONG).show()
                publishViewModel.dismiss()
                findNavController().navigateUp()
            }
            is PublishUiState.Error -> {
                Toast.makeText(requireContext(), s.message, Toast.LENGTH_LONG).show()
                publishViewModel.dismiss()
            }
            else -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}