package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.databinding.FragmentCheckpointListBinding
import kotlinx.coroutines.launch

class CheckpointListFragment : Fragment() {

    private var _binding: FragmentCheckpointListBinding? = null
    private val binding get() = _binding!!

    private val gameId: String get() = requireArguments().getString("gameId").orEmpty()

    private val viewModel: CheckpointListViewModel by viewModels {
        CheckpointListViewModelFactory(CampusQuestApplication.instance.gameRepository, gameId)
    }

    private val adapter = CheckpointAdapter(
        onEdit = { cp -> openEditor(cp.id) },
        onDelete = { cp ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete checkpoint?")
                .setMessage("This checkpoint will be removed from this game.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ -> viewModel.delete(cp.id) }
                .show()
        },
        onUp = { cp -> viewModel.moveUp(cp.id) },
        onDown = { cp -> viewModel.moveDown(cp.id) }
    )

    private fun openEditor(checkpointId: String?) {
        val bundle = Bundle().apply {
            putString("gameId", gameId)
            if (checkpointId != null) putString("checkpointId", checkpointId)
        }
        findNavController().navigate(R.id.action_checkpointList_to_checkpointEditor, bundle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckpointListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        // Only the quest's creator ever reaches this screen (Creator Studio lists their own quests),
        // and the repository checks again before deleting.
        binding.toolbar.menu.add(R.string.quest_instruction_card).setOnMenuItemClickListener {
            findNavController().navigate(R.id.storySetupFragment, Bundle().apply { putString("gameId", gameId) })
            true
        }
        binding.toolbar.menu.add(R.string.quest_delete).setOnMenuItemClickListener {
            QuestDeletion.confirmAndDelete(this, gameId) { findNavController().navigateUp() }
            true
        }
        binding.rvCheckpoints.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCheckpoints.adapter = adapter
        binding.btnAddCheckpoint.setOnClickListener { openEditor(null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    adapter.submitList(s.checkpoints)
                    // The Golden Tree has exactly three stops.
                    binding.btnAddCheckpoint.isEnabled = s.checkpoints.size < com.campusquest.ui.creator.validation.CreatorLimits.MAX_CHECKPOINTS
                    binding.tvEmpty.visibility =
                        if (s.checkpoints.isEmpty() && !s.isLoading) View.VISIBLE else View.GONE
                    s.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.onErrorShown()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load() // refresh after coming back from the editor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}