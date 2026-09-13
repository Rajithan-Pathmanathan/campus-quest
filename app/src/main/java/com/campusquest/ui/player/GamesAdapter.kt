package com.campusquest.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.R
import com.campusquest.databinding.ItemGameCardBinding
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus

/**
 * RecyclerView Adapter for exploring published quests using DiffUtil.
 */
class GamesAdapter(
    private val onGameClicked: (Game) -> Unit
) : ListAdapter<Game, GamesAdapter.GameViewHolder>(GameDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GameViewHolder(binding, onGameClicked)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GameViewHolder(
        private val binding: ItemGameCardBinding,
        private val onGameClicked: (Game) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(game: Game) {
            val context = binding.root.context

            binding.tvGameTitle.text = game.title
            binding.tvGameCreator.text = "By ${game.creatorName}"
            binding.tvGameDescription.text = game.description
            binding.tvCheckpointCountBadge.text = "📍 ${game.checkpointCount} Checkpoints"

            // Status styling
            when (game.status) {
                GameStatus.PUBLISHED -> {
                    binding.tvGameStatusBadge.text = "PUBLISHED"
                    binding.tvGameStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_emerald)
                    binding.tvGameStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_emerald))
                }
                GameStatus.DRAFT -> {
                    binding.tvGameStatusBadge.text = "DRAFT"
                    binding.tvGameStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_gold)
                    binding.tvGameStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.secondary_amber))
                }
                GameStatus.CLOSED -> {
                    binding.tvGameStatusBadge.text = "CLOSED"
                    binding.tvGameStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_cyan)
                    binding.tvGameStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_closed))
                }
            }

            binding.btnExploreQuest.setOnClickListener {
                onGameClicked(game)
            }

            binding.root.setOnClickListener {
                onGameClicked(game)
            }
        }
    }

    object GameDiffCallback : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem == newItem
        }
    }
}
