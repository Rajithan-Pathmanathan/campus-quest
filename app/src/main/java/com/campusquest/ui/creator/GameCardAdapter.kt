package com.campusquest.ui.creator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.databinding.ItemGameCardBinding
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus

class GameCardAdapter(
    private val onOpen: (Game) -> Unit
) : ListAdapter<Game, GameCardAdapter.Holder>(Diff) {

    class Holder(val binding: ItemGameCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemGameCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val game = getItem(position)
        with(holder.binding) {
            tvGameTitle.text = game.title
            tvGameCreator.text = when (game.status) {
                GameStatus.DRAFT -> "Draft"
                GameStatus.PUBLISHED -> "Published"
                GameStatus.CLOSED -> "Closed"
            }
            tvGameDescription.text = game.description
            tvCheckpointCount.text = "📍 ${game.checkpointCount} Checkpoints"
            btnViewDetails.text = if (game.status == GameStatus.DRAFT) "Edit" else "View"
            btnViewDetails.setOnClickListener { onOpen(game) }
            root.setOnClickListener { onOpen(game) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(a: Game, b: Game) = a.id == b.id
        override fun areContentsTheSame(a: Game, b: Game) = a == b
    }
}