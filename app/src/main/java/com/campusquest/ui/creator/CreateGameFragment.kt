package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusquest.R
import com.campusquest.databinding.FragmentCreateGameBinding
import kotlinx.coroutines.launch

/**
 * Fragment orchestrating the Quest Creation Wizard and Checkpoint Discovery Sequence Builder.
 */
class CreateGameFragment : Fragment() {

    private var _binding: FragmentCreateGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateGameViewModel by viewModels {
        com.campusquest.ui.common.ViewModelFactory.from(this)
    }
    private lateinit var adapter: EditorCheckpointsAdapter

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

        val gameId = arguments?.getString("gameId")
        viewModel.loadQuest(gameId)

        setupToolbar()
        setupAdapter()
        setupInputs()
        setupButtons()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAdapter() {
        adapter = EditorCheckpointsAdapter(
            onRowClicked = { checkpoint ->
                val args = bundleOf(
                    "gameId" to viewModel.uiState.value.gameId,
                    "checkpointId" to checkpoint.id
                )
                findNavController().navigate(R.id.action_createGame_to_checkpointEditor, args)
            },
            onMoveUpClicked = { position ->
                viewModel.moveCheckpointUp(position)
            },
            onMoveDownClicked = { position ->
                viewModel.moveCheckpointDown(position)
            },
            onDeleteClicked = { checkpoint ->
                viewModel.removeCheckpoint(checkpoint.id)
                Toast.makeText(requireContext(), "Checkpoint removed from chain", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvEditorCheckpoints.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@CreateGameFragment.adapter
        }
    }

    private fun setupInputs() {
        binding.etGameTitle.doAfterTextChanged { text ->
            if (binding.etGameTitle.hasFocus()) {
                viewModel.updateTitle(text?.toString().orEmpty())
            }
        }

        binding.etGameDesc.doAfterTextChanged { text ->
            if (binding.etGameDesc.hasFocus()) {
                viewModel.updateDescription(text?.toString().orEmpty())
            }
        }
    }

    private fun setupButtons() {
        binding.btnAddCheckpoint.setOnClickListener {
            val args = bundleOf(
                "gameId" to viewModel.uiState.value.gameId,
                "checkpointId" to null
            )
            findNavController().navigate(R.id.action_createGame_to_checkpointEditor, args)
        }

        binding.btnSaveDraft.setOnClickListener {
            viewModel.saveDraft()
            Toast.makeText(requireContext(), "💾 Quest draft saved successfully!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        binding.btnPublish.setOnClickListener {
            val success = viewModel.publishQuest()
            if (success) {
                Toast.makeText(requireContext(), "🚀 Quest published to campus! Explorers can now discover it.", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            } else {
                val errorMsg = viewModel.uiState.value.errorMessage ?: "Cannot publish quest."
                Toast.makeText(requireContext(), "⚠️ $errorMsg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: CreateGameUiState) {
        if (!binding.etGameTitle.hasFocus() && binding.etGameTitle.text?.toString() != state.title) {
            binding.etGameTitle.setText(state.title)
        }

        if (!binding.etGameDesc.hasFocus() && binding.etGameDesc.text?.toString() != state.description) {
            binding.etGameDesc.setText(state.description)
        }

        adapter.submitList(state.checkpoints)

        // Sequence count badge
        val count = state.checkpointCount
        binding.tvSequenceRequirementBadge.text = "$count of 2 Required"
        if (count >= 2) {
            binding.tvSequenceRequirementBadge.setBackgroundResource(R.drawable.bg_badge_emerald)
            binding.tvSequenceRequirementBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary_emerald))
        } else {
            binding.tvSequenceRequirementBadge.setBackgroundResource(R.drawable.bg_badge_gold)
            binding.tvSequenceRequirementBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_secondary_amber))
        }

        // Empty view toggle
        binding.layoutEmptySequence.visibility = if (count == 0) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
