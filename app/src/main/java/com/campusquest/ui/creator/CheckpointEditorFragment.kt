package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentCheckpointEditorBinding
import kotlinx.coroutines.launch

/**
 * Fragment allowing creators to configure checkpoint geospatial parameters,
 * ambient light thresholds, and motion gesture requirements.
 */
class CheckpointEditorFragment : Fragment() {

    private var _binding: FragmentCheckpointEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CheckpointEditorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckpointEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gameId = arguments?.getString("gameId") ?: "TEMP_GAME"
        val checkpointId = arguments?.getString("checkpointId")
        viewModel.loadCheckpoint(gameId, checkpointId)

        setupToolbar()
        setupInputs()
        setupLightPresets()
        setupMotionChips()
        setupSaveButton()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupInputs() {
        binding.etCpName.doAfterTextChanged { text ->
            if (binding.etCpName.hasFocus()) {
                viewModel.updateIdentity(
                    name = text?.toString().orEmpty(),
                    clue = binding.etCpClue.text?.toString().orEmpty(),
                    lore = binding.etCpLore.text?.toString().orEmpty()
                )
            }
        }

        binding.etCpClue.doAfterTextChanged { text ->
            if (binding.etCpClue.hasFocus()) {
                viewModel.updateIdentity(
                    name = binding.etCpName.text?.toString().orEmpty(),
                    clue = text?.toString().orEmpty(),
                    lore = binding.etCpLore.text?.toString().orEmpty()
                )
            }
        }

        binding.etCpLore.doAfterTextChanged { text ->
            if (binding.etCpLore.hasFocus()) {
                viewModel.updateIdentity(
                    name = binding.etCpName.text?.toString().orEmpty(),
                    clue = binding.etCpClue.text?.toString().orEmpty(),
                    lore = text?.toString().orEmpty()
                )
            }
        }

        binding.etCpLat.doAfterTextChanged { text ->
            if (binding.etCpLat.hasFocus()) {
                val lat = text?.toString()?.toDoubleOrNull() ?: 0.0
                val lng = binding.etCpLng.text?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.updateCoordinates(lat, lng, binding.sliderRadius.value)
            }
        }

        binding.etCpLng.doAfterTextChanged { text ->
            if (binding.etCpLng.hasFocus()) {
                val lat = binding.etCpLat.text?.toString()?.toDoubleOrNull() ?: 0.0
                val lng = text?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.updateCoordinates(lat, lng, binding.sliderRadius.value)
            }
        }

        binding.sliderRadius.addOnChangeListener { _, value, fromUser ->
            binding.tvRadiusValue.text = "${value.toInt()}m"
            if (fromUser) {
                val lat = binding.etCpLat.text?.toString()?.toDoubleOrNull() ?: 0.0
                val lng = binding.etCpLng.text?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.updateCoordinates(lat, lng, value)
            }
        }

        binding.etCpMinLux.doAfterTextChanged { text ->
            if (binding.etCpMinLux.hasFocus()) {
                val min = text?.toString()?.toFloatOrNull() ?: 0f
                val max = binding.etCpMaxLux.text?.toString()?.toFloatOrNull() ?: 1000f
                viewModel.updateCustomLight(min, max)
            }
        }

        binding.etCpMaxLux.doAfterTextChanged { text ->
            if (binding.etCpMaxLux.hasFocus()) {
                val min = binding.etCpMinLux.text?.toString()?.toFloatOrNull() ?: 0f
                val max = text?.toString()?.toFloatOrNull() ?: 1000f
                viewModel.updateCustomLight(min, max)
            }
        }
    }

    private fun setupLightPresets() {
        binding.chipPresetDim.setOnClickListener {
            viewModel.applyLightPreset(LightPreset.DIM_SHADOW)
        }
        binding.chipPresetIndoor.setOnClickListener {
            viewModel.applyLightPreset(LightPreset.INDOOR_LAB)
        }
        binding.chipPresetDaylight.setOnClickListener {
            viewModel.applyLightPreset(LightPreset.DAYLIGHT)
        }
        binding.chipPresetSun.setOnClickListener {
            viewModel.applyLightPreset(LightPreset.HIGH_NOON)
        }
    }

    private fun setupMotionChips() {
        binding.chipMotionSweep.setOnClickListener { viewModel.updateMotionType("SWEEP") }
        binding.chipMotionShake.setOnClickListener { viewModel.updateMotionType("SHAKE") }
        binding.chipMotionTilt.setOnClickListener { viewModel.updateMotionType("TILT") }
        binding.chipMotionNone.setOnClickListener { viewModel.updateMotionType("NONE") }
    }

    private fun setupSaveButton() {
        binding.btnSaveCheckpoint.setOnClickListener {
            val cp = viewModel.saveCheckpoint()
            if (cp != null) {
                Toast.makeText(requireContext(), "💾 Checkpoint '${cp.name}' saved!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                val error = viewModel.uiState.value.errorMessage ?: "Cannot save checkpoint."
                Toast.makeText(requireContext(), "⚠️ $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: CheckpointEditorUiState) {
        binding.toolbar.title = if (state.isEditMode) "Edit Checkpoint" else "New Checkpoint"

        if (!binding.etCpName.hasFocus() && binding.etCpName.text?.toString() != state.name) {
            binding.etCpName.setText(state.name)
        }
        if (!binding.etCpClue.hasFocus() && binding.etCpClue.text?.toString() != state.clue) {
            binding.etCpClue.setText(state.clue)
        }
        if (!binding.etCpLore.hasFocus() && binding.etCpLore.text?.toString() != state.lore) {
            binding.etCpLore.setText(state.lore)
        }
        if (!binding.etCpLat.hasFocus() && binding.etCpLat.text?.toString() != state.lat.toString()) {
            binding.etCpLat.setText(state.lat.toString())
        }
        if (!binding.etCpLng.hasFocus() && binding.etCpLng.text?.toString() != state.lng.toString()) {
            binding.etCpLng.setText(state.lng.toString())
        }

        binding.sliderRadius.value = state.radiusM
        binding.tvRadiusValue.text = "${state.radiusM.toInt()}m"

        if (!binding.etCpMinLux.hasFocus() && binding.etCpMinLux.text?.toString() != state.minLux.toInt().toString()) {
            binding.etCpMinLux.setText(state.minLux.toInt().toString())
        }
        if (!binding.etCpMaxLux.hasFocus() && binding.etCpMaxLux.text?.toString() != state.maxLux.toInt().toString()) {
            binding.etCpMaxLux.setText(state.maxLux.toInt().toString())
        }

        // Highlight preset chip
        when (state.selectedPreset) {
            LightPreset.DIM_SHADOW -> binding.chipPresetDim.isChecked = true
            LightPreset.INDOOR_LAB -> binding.chipPresetIndoor.isChecked = true
            LightPreset.DAYLIGHT -> binding.chipPresetDaylight.isChecked = true
            LightPreset.HIGH_NOON -> binding.chipPresetSun.isChecked = true
            LightPreset.CUSTOM -> binding.chipGroupLightPresets.clearCheck()
        }

        // Highlight motion chip
        when (state.motionType) {
            "SWEEP" -> binding.chipMotionSweep.isChecked = true
            "SHAKE" -> binding.chipMotionShake.isChecked = true
            "TILT" -> binding.chipMotionTilt.isChecked = true
            "NONE" -> binding.chipMotionNone.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
