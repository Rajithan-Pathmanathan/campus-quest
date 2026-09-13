package com.campusquest.ui.creator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.databinding.ItemEditorCheckpointRowBinding
import com.campusquest.domain.model.Checkpoint

/**
 * ListAdapter for the creator's checkpoint sequence builder, allowing re-ordering, editing, and deletion.
 */
class EditorCheckpointsAdapter(
    private val onRowClicked: (Checkpoint) -> Unit,
    private val onMoveUpClicked: (Int) -> Unit,
    private val onMoveDownClicked: (Int) -> Unit,
    private val onDeleteClicked: (Checkpoint) -> Unit
) : ListAdapter<Checkpoint, EditorCheckpointsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditorCheckpointRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position, itemCount)
    }

    inner class ViewHolder(
        private val binding: ItemEditorCheckpointRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(checkpoint: Checkpoint, position: Int, totalCount: Int) {
            binding.tvSequenceOrderBadge.text = "#${position + 1}"
            binding.tvRowCpName.text = checkpoint.name
            binding.tvRowClue.text = checkpoint.clue

            // Sensor chips
            binding.tvChipRadius.text = "📍 ${checkpoint.radiusM.toInt()}m"
            val light = checkpoint.lightSignature
            binding.tvChipLight.text = "💡 ${light.minLux.toInt()}-${light.maxLux.toInt()}lx"
            binding.tvChipMotion.text = "📳 ${checkpoint.motionType}"

            // Stepper controls
            binding.btnMoveUp.isEnabled = position > 0
            binding.btnMoveDown.isEnabled = position < totalCount - 1

            binding.root.setOnClickListener { onRowClicked(checkpoint) }
            binding.btnMoveUp.setOnClickListener { onMoveUpClicked(position) }
            binding.btnMoveDown.setOnClickListener { onMoveDownClicked(position) }
            binding.btnDeleteRow.setOnClickListener { onDeleteClicked(checkpoint) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Checkpoint>() {
            override fun areItemsTheSame(oldItem: Checkpoint, newItem: Checkpoint): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Checkpoint, newItem: Checkpoint): Boolean = oldItem == newItem
        }
    }
}
