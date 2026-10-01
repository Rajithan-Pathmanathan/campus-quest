package com.campusquest.ui.quest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.campusquest.databinding.FragmentQuestScanBinding
import com.campusquest.device.sensor.SensorFusionEngine
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuestScanFragment : Fragment() {

    private var _binding: FragmentQuestScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorFusionEngine: SensorFusionEngine

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

        sensorFusionEngine = SensorFusionEngine(requireContext())

        // Start listening with target signature (e.g. ambient light 150-800 lux) and SWEEP gesture
        sensorFusionEngine.startListening(
            gameId = gameId,
            checkpointId = checkpointId,
            targetSignature = LightSignature(150f, 800f),
            motionType = MotionType.SWEEP,
            gpsScore = 1.0f
        )

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                sensorFusionEngine.fusionResult.collectLatest { result ->
                    binding.pbFusionMeter.progress = result.progressPercent
                    if (result.canClaimDiscovery) {
                        binding.tvHudStatus.text = "✨ Relic Resonated 100%! Ready to Claim."
                        binding.btnSimulateUnlock.isEnabled = true
                    } else if (result.thresholdReached) {
                        binding.tvHudStatus.text = "Threshold Reached (${result.progressPercent}%)! Move closer for physical confirmation."
                        binding.btnSimulateUnlock.isEnabled = false
                    } else {
                        binding.tvHudStatus.text = "Progress: ${result.progressPercent}% - Align environmental sensors and gesture"
                        binding.btnSimulateUnlock.isEnabled = false
                    }
                }
            }

            launch {
                sensorFusionEngine.sensorState.collectLatest { sensorState ->
                    val luxStr = sensorState.currentLux?.let { "%.0f".format(it) } ?: "N/A"
                    val isNearStr = when (sensorState.isNear) {
                        true -> "Near"
                        false -> "Far"
                        null -> "N/A"
                    }
                    val motionStr = when (sensorState.motionDetected) {
                        true -> "Detected"
                        false -> "Pending"
                        null -> "N/A"
                    }
                    binding.tvSensorDiagnostics.text =
                        "Lux: $luxStr | Motion: $motionStr | Proximity: $isNearStr"
                }
            }
        }

        binding.btnSimulateUnlock.setOnClickListener {
            Toast.makeText(requireContext(), "Relic Claimed! Checkpoint Completed.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorFusionEngine.stopListening()
        _binding = null
    }
}
