package com.campusquest.ui.creator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.databinding.ItemCheckpointBinding
import com.campusquest.domain.model.Checkpoint

class CheckpointAdapter(
    private val onEdit: (Checkpoint) -> Unit,
    private val onDelete: (Checkpoint) -> Unit,
    private val onUp: (Checkpoint) -> Unit,
    private val onDown: (Checkpoint) -> Unit
) : ListAdapter<Checkpoint, CheckpointAdapter.Holder>(Diff) {

    class Holder(val binding: ItemCheckpointBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemCheckpointBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val cp = getItem(position)
        with(holder.binding) {
            tvCpTitle.text = "${position + 1}. ${cp.name}"
            tvCpDetails.text = "Radius: ${cp.radiusM.toInt()} m  |  Light: " +
                    "${cp.lightSignature.minLux.toInt()}-${cp.lightSignature.maxLux.toInt()} lux  |  ${cp.rarity}"
            btnUp.isEnabled = position > 0
            btnDown.isEnabled = position < itemCount - 1
            btnUp.setOnClickListener { onUp(cp) }
            btnDown.setOnClickListener { onDown(cp) }
            btnEdit.setOnClickListener { onEdit(cp) }
            btnDelete.setOnClickListener { onDelete(cp) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Checkpoint>() {
        override fun areItemsTheSame(a: Checkpoint, b: Checkpoint) = a.id == b.id
        override fun areContentsTheSame(a: Checkpoint, b: Checkpoint) = a == b
    }
}