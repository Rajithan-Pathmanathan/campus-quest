package com.campusquest.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.databinding.ItemCheckpointPreviewBinding
import com.campusquest.domain.model.Checkpoint

/**
 * RecyclerView adapter for displaying the ordered sequence of checkpoints in the Quest Detail screen.
 */
class CheckpointsPreviewAdapter :
    ListAdapter<Checkpoint, CheckpointsPreviewAdapter.ViewHolder>(CheckpointDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCheckpointPreviewBinding.inflate(
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
        private val binding: ItemCheckpointPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(checkpoint: Checkpoint) {
            binding.tvOrderBadge.text = "${checkpoint.order}"
            binding.tvCheckpointName.text = checkpoint.name
            binding.tvCheckpointClue.text = checkpoint.clue
            binding.tvRadiusChip.text = "📍 ${checkpoint.radiusM.toInt()}m"
            binding.tvMotionChip.text = "📳 ${checkpoint.motionType}"
        }
    }

    object CheckpointDiffCallback : DiffUtil.ItemCallback<Checkpoint>() {
        override fun areItemsTheSame(oldItem: Checkpoint, newItem: Checkpoint): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Checkpoint, newItem: Checkpoint): Boolean {
            return oldItem == newItem
        }
    }
}
