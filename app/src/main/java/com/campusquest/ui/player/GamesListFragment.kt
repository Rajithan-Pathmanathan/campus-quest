package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusquest.R
import com.campusquest.databinding.FragmentGamesListBinding
import com.campusquest.domain.model.GameStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Player Quest Discovery Screen.
 * Allows players to search, filter, and discover active quests across campus.
 */
class GamesListFragment : Fragment() {

    private var _binding: FragmentGamesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GamesListViewModel by viewModels()
    private lateinit var adapter: GamesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGamesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchAndFilters()
        observeUiState()

        binding.btnRetry.setOnClickListener {
            viewModel.refresh()
        }
    }

    private fun setupRecyclerView() {
        adapter = GamesAdapter { game ->
            val args = bundleOf("gameId" to game.id)
            findNavController().navigate(R.id.action_gamesList_to_gameDetail, args)
        }

        binding.rvGames.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGames.adapter = adapter
    }

    private fun setupSearchAndFilters() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when {
                checkedIds.contains(R.id.chip_published) -> GameStatus.PUBLISHED
                checkedIds.contains(R.id.chip_drafts) -> GameStatus.DRAFT
                else -> null // All
            }
            viewModel.onStatusFilterChanged(status)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // Progress Bar
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                // Empty State
                binding.layoutEmptyState.visibility = if (state.isEmpty) View.VISIBLE else View.GONE

                // Error State
                if (state.errorMessage != null) {
                    binding.layoutErrorState.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = state.errorMessage
                    binding.rvGames.visibility = View.GONE
                } else {
                    binding.layoutErrorState.visibility = View.GONE
                    binding.rvGames.visibility = if (state.isEmpty) View.GONE else View.VISIBLE
                }

                // Update List
                adapter.submitList(state.filteredGames)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
