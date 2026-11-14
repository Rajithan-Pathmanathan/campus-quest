package com.campusquest.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.R
import com.campusquest.databinding.FragmentLeaderboardBinding
import com.campusquest.domain.model.GameLeaderboardEntry
import kotlinx.coroutines.launch

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaderboardViewModel by viewModels()
    private val gameId: String get() = arguments?.getString("gameId").orEmpty()
    private val adapter = LeaderboardAdapter()

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

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = adapter
        binding.btnRetry.setOnClickListener { viewModel.load(gameId, force = true) }

        viewModel.load(gameId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: LeaderboardUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.rvLeaderboard.isVisible = !state.isLoading && state.entries.isNotEmpty()
        binding.tvEmpty.isVisible = state.isEmpty
        binding.layoutError.isVisible = state.error != null
        binding.tvErrorMessage.text = state.error
        adapter.submit(state.entries)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

        private var items: List<GameLeaderboardEntry> = emptyList()

        fun submit(newItems: List<GameLeaderboardEntry>) {
            items = newItems
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRank: TextView = view.findViewById(R.id.tv_rank)
            val tvPlayerName: TextView = view.findViewById(R.id.tv_player_name)
            val tvTimeScore: TextView = view.findViewById(R.id.tv_time_score)
            val tvPoints: TextView = view.findViewById(R.id.tv_points)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaderboard_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = items[position]
            holder.tvRank.text = "#${position + 1}"
            holder.tvPlayerName.text = entry.userName
            val mins = entry.completionTimeSeconds / 60
            val secs = entry.completionTimeSeconds % 60
            holder.tvTimeScore.text = "Completed in ${mins}m ${secs}s"
            holder.tvPoints.text = "${entry.score} pts"
        }

        override fun getItemCount() = items.size
    }
}