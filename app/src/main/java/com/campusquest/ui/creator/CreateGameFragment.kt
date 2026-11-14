package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

class CreateGameFragment : Fragment() {

    private var _binding: FragmentCreateGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateGameViewModel by viewModels {
        CreateGameViewModelFactory(
            repository = CampusQuestApplication.instance.gameRepository,
            // TODO: replace with the real logged-in user once we know where AuthRepository lives
            currentUserId = { "TEMP_CREATOR_ID" },
            currentUserName = { "Creator" }
        )
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
            Toast.makeText(requireContext(), "Publishing is coming soon.", Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}