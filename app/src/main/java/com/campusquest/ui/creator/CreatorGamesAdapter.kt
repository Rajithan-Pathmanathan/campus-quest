package com.campusquest.ui.creator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.R
import com.campusquest.databinding.ItemCreatorGameCardBinding
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus

/**
 * ListAdapter for rendering creator authored quests and drafts using DiffUtil.
 */
class CreatorGamesAdapter(
    private val onEditClicked: (Game) -> Unit,
    private val onPublishClicked: (Game) -> Unit,
    private val onDeleteClicked: (Game) -> Unit
) : ListAdapter<Game, CreatorGamesAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCreatorGameCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCreatorGameCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(game: Game) {
            val context = binding.root.context
            binding.tvCreatorGameTitle.text = game.title
            binding.tvCreatorGameDesc.text = game.description
            binding.tvCreatorCheckpointCount.text = "📍 ${game.checkpointCount} Checkpoints"

            if (game.status == GameStatus.PUBLISHED) {
                binding.tvCreatorGameStatus.text = "PUBLISHED ACTIVE"
                binding.tvCreatorGameStatus.setBackgroundResource(R.drawable.bg_badge_emerald)
                binding.tvCreatorGameStatus.setTextColor(ContextCompat.getColor(context, R.color.on_primary_emerald))
                binding.btnPublishQuest.visibility = View.GONE
                binding.btnEditQuest.text = "👁️ View Checkpoints"
            } else {
                binding.tvCreatorGameStatus.text = "DRAFT IN PROGRESS"
                binding.tvCreatorGameStatus.setBackgroundResource(R.drawable.bg_badge_gold)
                binding.tvCreatorGameStatus.setTextColor(ContextCompat.getColor(context, R.color.on_secondary_amber))
                binding.btnPublishQuest.visibility = View.VISIBLE
                binding.btnEditQuest.text = "✏️ Edit / Builder"
            }

            binding.btnEditQuest.setOnClickListener { onEditClicked(game) }
            binding.btnPublishQuest.setOnClickListener { onPublishClicked(game) }
            binding.btnDeleteQuest.setOnClickListener { onDeleteClicked(game) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Game>() {
            override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean = oldItem == newItem
        }
    }
}
