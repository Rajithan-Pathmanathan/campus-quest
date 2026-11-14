package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.CampusQuestApplication
import com.campusquest.databinding.FragmentCheckpointEditorBinding
import com.campusquest.domain.model.MotionType
import com.campusquest.ui.creator.validation.CreatorLimits
import kotlinx.coroutines.launch

class CheckpointEditorFragment : Fragment() {

    private var _binding: FragmentCheckpointEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CheckpointEditorViewModel by viewModels {
        CheckpointEditorViewModelFactory(
            repository = CampusQuestApplication.instance.gameRepository,
            gameId = requireArguments().getString("gameId").orEmpty(),
            checkpointId = arguments?.getString("checkpointId")
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckpointEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun EditText.setIfDifferent(value: String) {
        if (text?.toString() != value) setText(value)
    }

    private fun AutoCompleteTextView.setupDropdown(items: List<String>, onPick: (String) -> Unit) {
        setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items))
        setOnItemClickListener { _, _, position, _ -> onPick(items[position]) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.etCpName.doAfterTextChanged { t -> viewModel.update { it.copy(name = t?.toString().orEmpty()) } }
        binding.etLat.doAfterTextChanged { t -> viewModel.update { it.copy(lat = t?.toString().orEmpty()) } }
        binding.etLng.doAfterTextChanged { t -> viewModel.update { it.copy(lng = t?.toString().orEmpty()) } }
        binding.etRadius.doAfterTextChanged { t -> viewModel.update { it.copy(radiusM = t?.toString().orEmpty()) } }
        binding.etMinLux.doAfterTextChanged { t -> viewModel.update { it.copy(minLux = t?.toString().orEmpty()) } }
        binding.etMaxLux.doAfterTextChanged { t -> viewModel.update { it.copy(maxLux = t?.toString().orEmpty()) } }
        binding.etClue.doAfterTextChanged { t -> viewModel.update { it.copy(clue = t?.toString().orEmpty()) } }
        binding.etLore.doAfterTextChanged { t -> viewModel.update { it.copy(lore = t?.toString().orEmpty()) } }
        binding.etOrder.doAfterTextChanged { t -> viewModel.update { it.copy(order = t?.toString().orEmpty()) } }

        binding.actMotion.setupDropdown(MotionType.values().map { it.name }) { pick ->
            viewModel.update { it.copy(motionType = pick) }
        }
        binding.actRarity.setupDropdown(CreatorLimits.RARITIES) { pick ->
            viewModel.update { it.copy(rarity = pick) }
        }

        binding.btnSaveCheckpoint.setOnClickListener { viewModel.save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    binding.etCpName.setIfDifferent(s.name)
                    binding.etLat.setIfDifferent(s.lat)
                    binding.etLng.setIfDifferent(s.lng)
                    binding.etRadius.setIfDifferent(s.radiusM)
                    binding.etMinLux.setIfDifferent(s.minLux)
                    binding.etMaxLux.setIfDifferent(s.maxLux)
                    binding.etClue.setIfDifferent(s.clue)
                    binding.etLore.setIfDifferent(s.lore)
                    binding.etOrder.setIfDifferent(s.order)
                    binding.actMotion.setText(s.motionType, false)
                    binding.actRarity.setText(s.rarity, false)

                    binding.tilCpName.error = s.errors["name"]
                    binding.tilLat.error = s.errors["lat"]
                    binding.tilLng.error = s.errors["lng"]
                    binding.tilRadius.error = s.errors["radius"]
                    binding.tilMinLux.error = s.errors["lux"]
                    binding.tilClue.error = s.errors["clue"]
                    binding.tilOrder.error = s.errors["order"]
                    binding.tilRarity.error = s.errors["rarity"]

                    binding.btnSaveCheckpoint.isEnabled = !s.isSaving
                    binding.btnSaveCheckpoint.text = if (s.isSaving) "Saving..." else "Save Checkpoint"

                    s.saveError?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                    if (s.saved) {
                        Toast.makeText(requireContext(), "Checkpoint saved", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}