package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusquest.R
import com.campusquest.databinding.FragmentCreatorDashboardBinding
import kotlinx.coroutines.launch

/**
 * Fragment serving as the Creator Studio command center.
 * Displays authoring analytics, lists drafts and published quests, and initiates the creation wizard.
 */
class CreatorDashboardFragment : Fragment() {

    private var _binding: FragmentCreatorDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreatorDashboardViewModel by viewModels {
        com.campusquest.ui.common.ViewModelFactory.from(this)
    }

    private lateinit var draftsAdapter: CreatorGamesAdapter
    private lateinit var publishedAdapter: CreatorGamesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatorDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupButtons()
        observeUiState()

        viewModel.loadAuthoredGames()
    }

    private fun setupAdapters() {
        draftsAdapter = CreatorGamesAdapter(
            onEditClicked = { game ->
                val args = bundleOf("gameId" to game.id)
                findNavController().navigate(R.id.action_creator_to_createGame, args)
            },
            onPublishClicked = { game ->
                viewModel.publishGame(game.id)
                Toast.makeText(requireContext(), "🚀 Quest '${game.title}' published!", Toast.LENGTH_SHORT).show()
            },
            onDeleteClicked = { game ->
                viewModel.deleteGame(game.id)
                Toast.makeText(requireContext(), "🗑️ Draft '${game.title}' deleted.", Toast.LENGTH_SHORT).show()
            }
        )

        publishedAdapter = CreatorGamesAdapter(
            onEditClicked = { game ->
                val args = bundleOf("gameId" to game.id)
                findNavController().navigate(R.id.action_creator_to_createGame, args)
            },
            onPublishClicked = {},
            onDeleteClicked = { game ->
                viewModel.deleteGame(game.id)
                Toast.makeText(requireContext(), "🗑️ Quest '${game.title}' removed.", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvDraftGames.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = draftsAdapter
        }

        binding.rvPublishedGames.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = publishedAdapter
        }
    }

    private fun setupButtons() {
        binding.btnCreateNewQuest.setOnClickListener {
            val args = bundleOf("gameId" to null)
            findNavController().navigate(R.id.action_creator_to_createGame, args)
        }

        binding.btnSwitchPlayer.setOnClickListener {
            findNavController().navigate(R.id.action_creator_to_playerDashboard)
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

    private fun renderUiState(state: CreatorDashboardUiState) {
        binding.toolbar.subtitle = "${state.creatorName} • Quest Architect"

        // Metrics
        binding.tvMetricTotalQuests.text = state.totalQuests.toString()
        binding.tvMetricPublishedQuests.text = state.publishedGames.size.toString()
        binding.tvMetricTotalCheckpoints.text = state.totalCheckpointsCreated.toString()

        // Drafts section
        draftsAdapter.submitList(state.draftGames)
        binding.tvDraftsHeader.visibility = if (state.hasDrafts) View.VISIBLE else View.GONE
        binding.rvDraftGames.visibility = if (state.hasDrafts) View.VISIBLE else View.GONE

        // Published section
        publishedAdapter.submitList(state.publishedGames)
        binding.tvPublishedHeader.visibility = if (state.hasPublished) View.VISIBLE else View.GONE
        binding.rvPublishedGames.visibility = if (state.hasPublished) View.VISIBLE else View.GONE

        // Empty state
        if (state.totalQuests == 0 && !state.isLoading) {
            binding.layoutEmptyCreator.visibility = View.VISIBLE
        } else {
            binding.layoutEmptyCreator.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
