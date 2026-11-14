package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.R
import com.campusquest.databinding.FragmentGamesListBinding
import com.campusquest.domain.model.Game
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class GamesListFragment : Fragment() {

    private var _binding: FragmentGamesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GamesViewModel by viewModels()
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

        adapter = GamesAdapter { game ->
            val bundle = Bundle().apply { putString("gameId", game.id) }
            findNavController().navigate(R.id.action_gamesList_to_gameDetail, bundle)
        }
        binding.rvGames.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGames.adapter = adapter

        binding.btnRetry.setOnClickListener { viewModel.loadGames() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: GamesUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.rvGames.visibility = if (!state.isLoading && state.error == null && state.games.isNotEmpty()) View.VISIBLE else View.GONE
        binding.layoutEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        binding.layoutError.visibility = if (state.error != null) View.VISIBLE else View.GONE

        if (state.error != null) {
            binding.tvErrorMessage.text = state.error
        }

        adapter.submitList(state.games)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class GamesAdapter(
        private val onClick: (Game) -> Unit
    ) : RecyclerView.Adapter<GamesAdapter.ViewHolder>() {

        private var items: List<Game> = emptyList()

        fun submitList(newItems: List<Game>) {
            items = newItems
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tv_game_title)
            val tvCreator: TextView = view.findViewById(R.id.tv_game_creator)
            val tvDescription: TextView = view.findViewById(R.id.tv_game_description)
            val tvCheckpointCount: TextView = view.findViewById(R.id.tv_checkpoint_count)
            val btnViewDetails: MaterialButton = view.findViewById(R.id.btn_view_details)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val game = items[position]
            holder.tvTitle.text = game.title
            holder.tvCreator.text = "By ${game.creatorName}"
            holder.tvDescription.text = game.description
            holder.tvCheckpointCount.text = "📍 ${game.checkpointCount} Checkpoints"
            holder.btnViewDetails.setOnClickListener { onClick(game) }
        }

        override fun getItemCount() = items.size
    }
}