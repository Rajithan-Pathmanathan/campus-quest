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

        // Start listening with target signature (e.g. ambient light 150-800 lux)
        sensorFusionEngine.startListening(LightSignature(150f, 800f), "SWEEP")

        viewLifecycleOwner.lifecycleScope.launch {
            sensorFusionEngine.fusionState.collectLatest { state ->
                binding.pbFusionMeter.progress = state.fusionProgressPercent
                binding.tvSensorDiagnostics.text =
                    "Accel: ${"%.1f".format(state.currentAcceleration)} m/s² | Lux: ${"%.0f".format(state.currentLux)} | Proximity: ${if (state.proximityVerified) "Near" else "Far"}"

                if (state.isFullyVerified) {
                    binding.tvHudStatus.text = "✨ Checkpoint Resonated 100%! Ready to Claim."
                    binding.btnSimulateUnlock.isEnabled = true
                } else {
                    binding.tvHudStatus.text = "Progress: ${state.fusionProgressPercent}% - Align sensors and gesture"
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
