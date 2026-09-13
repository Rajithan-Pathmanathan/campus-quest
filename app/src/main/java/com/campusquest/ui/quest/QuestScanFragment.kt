package com.campusquest.ui.quest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentQuestScanBinding
import com.campusquest.device.sensor.SensorFusionEngine
import com.campusquest.domain.model.MotionType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment rendering the Live Discovery HUD, multi-sensor resonance telemetry,
 * real-time progress meter, and physical proximity unlock gate.
 */
class QuestScanFragment : Fragment() {

    private var _binding: FragmentQuestScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuestScanViewModel by viewModels()
    private var sensorFusionEngine: SensorFusionEngine? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gameId = arguments?.getString("gameId") ?: "GAME_HERITAGE_001"
        val checkpointId = arguments?.getString("checkpointId") ?: "CP_BELL_TOWER"

        viewModel.loadScanContext(gameId, checkpointId)
        setupSensorListeners(gameId, checkpointId)
        setupButtons()
        observeUiState()
    }

    private fun setupSensorListeners(gameId: String, checkpointId: String) {
        try {
            val engine = SensorFusionEngine(requireContext())
            sensorFusionEngine = engine

            val cp = viewModel.uiState.value.checkpoint
            val sig = cp?.lightSignature ?: com.campusquest.domain.model.LightSignature(150f, 800f)
            val motion = try {
                MotionType.valueOf(cp?.motionType ?: "SWEEP")
            } catch (_: Exception) {
                MotionType.SWEEP
            }

            engine.startListening(
                gameId = gameId,
                checkpointId = checkpointId,
                targetSignature = sig,
                motionType = motion,
                gpsScore = 1.0f
            )

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        engine.sensorState.collectLatest { state ->
                            state.currentLux?.let { viewModel.onLightReadingChanged(it) }
                            state.motionDetected?.let { viewModel.onMotionDetected(it) }
                            state.isNear?.let { viewModel.onProximityChanged(it) }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback for non-sensor / headless testing
        }
    }

    private fun setupButtons() {
        binding.btnClaimDiscovery.setOnClickListener {
            val success = viewModel.claimDiscovery()
            if (success) {
                Toast.makeText(
                    requireContext(),
                    "✨ Relic Discovered! Checkpoint Completed (+100 pts).",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().navigateUp()
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

    private fun renderUiState(state: QuestScanUiState) {
        val cp = state.checkpoint
        binding.tvHudTitle.text = cp?.name ?: "Scanning Relic"
        binding.tvHudClue.text = "Clue: ${cp?.clue ?: "Searching signals..."}"
        binding.tvHudStatusBadge.text = state.statusBadgeText

        // Reticle rotation feedback
        binding.ivReticle.rotation = (state.progressPercent * 3.6f)

        // Sensor Cards
        binding.tvCardGpsScore.text = "${(state.gpsScore * 100).toInt()}% Fix"

        val luxText = state.currentLux?.let { "${it.toInt()} lx" } ?: "Aligning..."
        binding.tvCardLightScore.text = "$luxText (${(state.lightScore * 100).toInt()}%)"

        val motionText = if (state.motionDetected) "Detected" else "Pending"
        binding.tvCardMotionStatus.text = "${cp?.motionType ?: "SWEEP"}: $motionText"

        val proxText = if (state.proximityNear) "NEAR Confirmed" else "FAR"
        binding.tvCardProximityStatus.text = proxText
        if (state.proximityNear) {
            binding.tvCardProximityStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.primary_emerald)
            )
        } else {
            binding.tvCardProximityStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
        }

        // Progress meter
        binding.pbFusionMeter.progress = state.progressPercent
        binding.tvFusionPct.text = "${state.progressPercent}%"

        // Claim button
        binding.btnClaimDiscovery.isEnabled = state.canClaimDiscovery
        if (state.canClaimDiscovery) {
            binding.tvHudStatusBadge.setBackgroundResource(R.drawable.bg_badge_emerald)
            binding.tvHudStatusBadge.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.on_primary_emerald)
            )
        } else {
            binding.tvHudStatusBadge.setBackgroundResource(R.drawable.bg_badge_gold)
            binding.tvHudStatusBadge.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.on_secondary_amber)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorFusionEngine?.stopListening()
        sensorFusionEngine = null
        _binding = null
    }
}
