package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.data.mock.MockDataCatalog
import com.campusquest.databinding.FragmentGamesListBinding
import com.campusquest.domain.model.Game
import com.google.android.material.button.MaterialButton

class GamesListFragment : Fragment() {

    private var _binding: FragmentGamesListBinding? = null
    private val binding get() = _binding!!

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

        binding.rvGames.layoutManager = LinearLayoutManager(requireContext())
        val games = MockDataCatalog.allSampleGames
        binding.rvGames.adapter = GamesAdapter(games) { game ->
            val bundle = Bundle().apply {
                putString("gameId", game.id)
            }
            findNavController().navigate(R.id.action_gamesList_to_gameDetail, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class GamesAdapter(
        private val items: List<Game>,
        private val onClick: (Game) -> Unit
    ) : RecyclerView.Adapter<GamesAdapter.ViewHolder>() {

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
