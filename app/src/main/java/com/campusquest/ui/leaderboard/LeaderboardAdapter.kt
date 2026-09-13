package com.campusquest.ui.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.databinding.ItemLeaderboardEntryBinding
import com.campusquest.domain.model.GameLeaderboardEntry

/**
 * ListAdapter for rendering leaderboard ranked table rows (#4 and below) using DiffUtil.
 */
class LeaderboardAdapter : ListAdapter<GameLeaderboardEntry, LeaderboardAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaderboardEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemLeaderboardEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: GameLeaderboardEntry) {
            binding.tvRank.text = "#${entry.rank}"
            binding.tvPlayerName.text = entry.userName

            val mins = entry.completionTimeSeconds / 60
            val secs = entry.completionTimeSeconds % 60
            binding.tvTimeScore.text = "⏱️ ${mins}m ${secs}s"
            binding.tvPoints.text = "${entry.score} pts"
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GameLeaderboardEntry>() {
            override fun areItemsTheSame(
                oldItem: GameLeaderboardEntry,
                newItem: GameLeaderboardEntry
            ): Boolean = oldItem.userId == newItem.userId

            override fun areContentsTheSame(
                oldItem: GameLeaderboardEntry,
                newItem: GameLeaderboardEntry
            ): Boolean = oldItem == newItem
        }
    }
}
