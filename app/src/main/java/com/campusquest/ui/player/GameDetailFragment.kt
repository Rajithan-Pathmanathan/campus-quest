package com.campusquest.ui.player

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
import com.campusquest.databinding.FragmentGameDetailBinding
import com.campusquest.domain.model.GameStatus
import kotlinx.coroutines.launch

class GameDetailFragment : Fragment() {

    private var _binding: FragmentGameDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameDetailViewModel by viewModels()
    private val gameId: String get() = arguments?.getString("gameId").orEmpty()

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

        viewModel.load(gameId)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnRetry.setOnClickListener { viewModel.load(gameId, force = true) }
        binding.btnJoin.setOnClickListener { viewModel.join() }

        binding.btnStartQuest.setOnClickListener {
            findNavController().navigate(
                R.id.action_gameDetail_to_campusMap,
                Bundle().apply { putString("gameId", gameId) }
            )
        }
        binding.btnViewLeaderboard.setOnClickListener {
            findNavController().navigate(
                R.id.action_gameDetail_to_leaderboard,
                Bundle().apply { putString("gameId", gameId) }
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: GameDetailUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.layoutError.isVisible = !state.isLoading && state.error != null
        binding.layoutContent.isVisible = !state.isLoading && state.game != null
        binding.tvErrorMessage.text = state.error

        state.game?.let { game ->
            binding.tvTitle.text = game.title
            binding.tvCreator.text = "Created by: ${game.creatorName}"
            binding.tvStatus.text = game.status.name
            binding.tvCheckpointCount.text = "📍 ${game.checkpointCount} Checkpoints"
            binding.tvDescription.text = game.description

            val joined = state.joinStatus == JoinStatus.JOINED
            val joining = state.joinStatus == JoinStatus.JOINING
            val canJoin = game.status == GameStatus.PUBLISHED

            binding.btnJoin.isVisible = !joined
            binding.btnJoin.isEnabled = canJoin && !joining
            binding.btnJoin.text = when {
                !canJoin -> "Not available"
                joining -> "Joining…"
                state.joinStatus == JoinStatus.FAILED -> "Try again"
                else -> "Join Quest"
            }
            binding.btnStartQuest.isVisible = joined
        }

        binding.tvJoinError.isVisible = state.joinError != null
        binding.tvJoinError.text = state.joinError
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}