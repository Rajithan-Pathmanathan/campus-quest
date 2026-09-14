package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusquest.R
import com.campusquest.databinding.FragmentGameDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Quest Details and Checkpoint Overview Screen.
 * Displays quest lore, sequential checkpoint chain, and start/leaderboard actions.
 */
class GameDetailFragment : Fragment() {

    private var _binding: FragmentGameDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameDetailViewModel by viewModels {
        com.campusquest.ui.common.ViewModelFactory.from(this)
    }
    private lateinit var checkpointsAdapter: CheckpointsPreviewAdapter

    private var currentGameId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentGameId = arguments?.getString("gameId") ?: "GAME_HERITAGE_001"

        setupRecyclerView()
        observeUiState()

        viewModel.loadGameDetails(currentGameId)

        binding.btnStartQuest.setOnClickListener {
            viewModel.joinQuest()
            val args = bundleOf("gameId" to currentGameId)
            findNavController().navigate(R.id.action_gameDetail_to_campusMap, args)
        }

        binding.btnViewLeaderboard.setOnClickListener {
            val args = bundleOf("gameId" to currentGameId)
            findNavController().navigate(R.id.action_gameDetail_to_leaderboard, args)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadGameDetails(currentGameId)
        }
    }

    private fun setupRecyclerView() {
        checkpointsAdapter = CheckpointsPreviewAdapter()
        binding.rvCheckpoints.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCheckpoints.adapter = checkpointsAdapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                if (state.errorMessage != null) {
                    binding.layoutErrorState.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = state.errorMessage
                    binding.scrollContent.visibility = View.GONE
                } else {
                    binding.layoutErrorState.visibility = View.GONE
                    binding.scrollContent.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                }

                state.game?.let { game ->
                    binding.tvDetailTitle.text = game.title
                    binding.tvDetailCreator.text = "Created by ${game.creatorName}"
                    binding.tvDetailDescription.text = game.description
                    binding.tvDetailStatusBadge.text = game.status.name
                    binding.tvDetailCheckpointBadge.text = "📍 ${state.checkpoints.size} Checkpoints"
                }

                checkpointsAdapter.submitList(state.checkpoints)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
