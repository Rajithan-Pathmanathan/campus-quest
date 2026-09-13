package com.campusquest.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusquest.databinding.FragmentLeaderboardBinding
import com.campusquest.domain.model.GameLeaderboardEntry
import kotlinx.coroutines.launch

/**
 * Fragment displaying quest leaderboard rankings, the top-3 champions podium,
 * current player rank status, and ranked challenger rows.
 */
class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaderboardViewModel by viewModels()
    private val leaderboardAdapter = LeaderboardAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gameId = arguments?.getString("gameId") ?: "GAME_HERITAGE_001"
        viewModel.loadLeaderboard(gameId)

        setupToolbar()
        setupRecyclerView()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.rvLeaderboard.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaderboardAdapter
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

    private fun renderUiState(state: LeaderboardUiState) {
        if (state.gameTitle.isNotBlank()) {
            binding.toolbar.subtitle = state.gameTitle
        }

        // Current Player Standing Card
        val player = state.currentPlayerEntry
        if (player != null) {
            binding.cardPlayerStanding.visibility = View.VISIBLE
            binding.tvPlayerStandingRank.text = "#${player.rank}"
            binding.tvPlayerStandingName.text = "${player.userName} (You)"
            binding.tvPlayerStandingScore.text = "${player.score} pts"
        } else {
            binding.cardPlayerStanding.visibility = View.GONE
        }

        // Top 3 Podium Card
        if (state.podiumEntries.isNotEmpty()) {
            binding.cardPodium.visibility = View.VISIBLE

            // 1st Place Champion
            state.firstPlace?.let { first ->
                binding.podiumFirstPlace.visibility = View.VISIBLE
                binding.tvPodium1Name.text = first.userName
                binding.tvPodium1Pts.text = "${first.score} pts"
                binding.tvPodium1Time.text = formatTime(first.completionTimeSeconds)
            } ?: run {
                binding.podiumFirstPlace.visibility = View.INVISIBLE
            }

            // 2nd Place
            state.secondPlace?.let { second ->
                binding.podiumSecondPlace.visibility = View.VISIBLE
                binding.tvPodium2Name.text = second.userName
                binding.tvPodium2Pts.text = "${second.score} pts"
                binding.tvPodium2Time.text = formatTime(second.completionTimeSeconds)
            } ?: run {
                binding.podiumSecondPlace.visibility = View.INVISIBLE
            }

            // 3rd Place
            state.thirdPlace?.let { third ->
                binding.podiumThirdPlace.visibility = View.VISIBLE
                binding.tvPodium3Name.text = third.userName
                binding.tvPodium3Pts.text = "${third.score} pts"
                binding.tvPodium3Time.text = formatTime(third.completionTimeSeconds)
            } ?: run {
                binding.podiumThirdPlace.visibility = View.INVISIBLE
            }
        } else {
            binding.cardPodium.visibility = View.GONE
        }

        // Challengers List (Rank 4+)
        leaderboardAdapter.submitList(state.rankedEntries)
        binding.tvChallengersHeader.visibility = if (state.rankedEntries.isNotEmpty()) View.VISIBLE else View.GONE
        binding.rvLeaderboard.visibility = if (state.rankedEntries.isNotEmpty()) View.VISIBLE else View.GONE

        // Empty state
        if (state.totalEntries == 0 && !state.isLoading) {
            binding.layoutEmptyLeaderboard.visibility = View.VISIBLE
        } else {
            binding.layoutEmptyLeaderboard.visibility = View.GONE
        }
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "${mins}m ${secs}s"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
