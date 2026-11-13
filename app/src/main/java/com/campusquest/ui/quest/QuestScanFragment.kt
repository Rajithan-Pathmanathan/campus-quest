package com.campusquest.ui.quest

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.campusquest.CampusQuestApplication
import com.campusquest.databinding.FragmentQuestScanBinding
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LocationPermissionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuestScanFragment : Fragment() {

    private var _binding: FragmentQuestScanBinding? = null
    private val binding get() = _binding!!

    private val app get() = requireActivity().application as CampusQuestApplication
    private val locationRepository get() = app.locationRepository
    private val sensorRepository get() = app.sensorRepository

    private var checkpoint: Checkpoint? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationRepository.updatePermissionState(
            if (granted) LocationPermissionState.Granted else LocationPermissionState.Denied
        )
        if (granted) {
            locationRepository.startTracking()
        } else {
            _binding?.tvHudStatus?.text = "Location permission is required to scan for relics."
        }
    }

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

        viewLifecycleOwner.lifecycleScope.launch {
            val cp = app.gameRepository.getGameCheckpoints(gameId).firstOrNull { it.id == checkpointId }
            if (cp == null) {
                binding.tvHudStatus.text = "Checkpoint not found."
                return@launch
            }
            checkpoint = cp
            sensorRepository.startScanSession(cp)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                sensorRepository.fusionResult.collectLatest { result ->
                    binding.pbFusionMeter.progress = result.progressPercent
                    binding.btnSimulateUnlock.isEnabled = result.canClaimDiscovery
                    binding.tvHudStatus.text = when {
                        result.canClaimDiscovery -> "✨ Relic Resonated! Ready to Claim."
                        result.thresholdReached -> "Threshold Reached (${result.progressPercent}%)! Move closer for physical confirmation."
                        result.gpsScore == 0f -> "Progress: ${result.progressPercent}% - Get closer to the checkpoint"
                        else -> "Progress: ${result.progressPercent}% - Align environmental sensors and gesture"
                    }
                }
            }

            launch {
                sensorRepository.sensorState.collectLatest { sensorState ->
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
            val cp = checkpoint ?: return@setOnClickListener
            if (!sensorRepository.fusionResult.value.canClaimDiscovery) return@setOnClickListener
            binding.btnSimulateUnlock.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                app.gameRepository.recordDiscovery(cp.gameId, cp.id, System.currentTimeMillis())
                Toast.makeText(requireContext(), "Relic Claimed! Checkpoint Completed.", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        locationPermissionRequest.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        checkpoint?.let { sensorRepository.startScanSession(it) }
    }

    override fun onStop() {
        super.onStop()
        sensorRepository.stopScanSession()
        locationRepository.stopTracking()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
