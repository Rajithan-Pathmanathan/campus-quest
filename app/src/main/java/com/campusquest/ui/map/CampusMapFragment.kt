package com.campusquest.ui.map

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentCampusMapBinding
import com.campusquest.domain.model.GeofenceState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

/**
 * Fragment rendering the interactive Google Maps view, radar compass telemetry,
 * real-time geofence visual indicators, and the discovery scan activation trigger.
 */
class CampusMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CampusMapViewModel by viewModels {
        com.campusquest.ui.common.ViewModelFactory.from(this)
    }
    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gameId = arguments?.getString("gameId") ?: "GAME_HERITAGE_001"
        viewModel.loadMapData(gameId)

        // Initialize Map Fragment safely
        try {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        } catch (_: Exception) {
            // Safe fallback if Google Play services / map fragment unavailable in test environment
        }

        setupButtons()
        observeUiState()
    }

    private fun setupButtons() {
        binding.btnPrevWaypoint.setOnClickListener {
            val currentIndex = viewModel.uiState.value.activeCheckpointIndex
            if (currentIndex > 0) {
                viewModel.selectCheckpoint(currentIndex - 1)
            }
        }

        binding.btnNextWaypoint.setOnClickListener {
            val currentIndex = viewModel.uiState.value.activeCheckpointIndex
            val total = viewModel.uiState.value.checkpoints.size
            if (currentIndex < total - 1) {
                viewModel.selectCheckpoint(currentIndex + 1)
            }
        }

        binding.fabEnterScan.setOnClickListener {
            val state = viewModel.uiState.value
            val activeCp = state.activeCheckpoint ?: return@setOnClickListener
            val bundle = Bundle().apply {
                putString("gameId", state.gameId ?: "GAME_HERITAGE_001")
                putString("checkpointId", activeCp.id)
            }
            findNavController().navigate(R.id.action_campusMap_to_questScan, bundle)
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

    private fun renderUiState(state: CampusMapUiState) {
        binding.tvMapQuestTitle.text = state.gameTitle

        // Telemetry GPS status
        val loc = state.playerLocation
        if (loc != null) {
            binding.tvGpsStatus.text = "🛰️ GPS Active • Accuracy ${loc.formattedAccuracy()}"
        } else {
            binding.tvGpsStatus.text = "🛰️ Acquiring GPS telemetry..."
        }

        // Radar Compass Needle Rotation
        val bearing = state.targetBearingDegrees?.toFloat() ?: 0f
        binding.ivCompassNeedle.rotation = bearing

        // Active Checkpoint Card
        val activeCp = state.activeCheckpoint
        if (activeCp != null) {
            binding.cardTargetHud.visibility = View.VISIBLE
            val totalCount = state.checkpoints.size
            binding.tvCheckpointSequenceBadge.text = "WAYPOINT ${state.activeCheckpointIndex + 1} OF $totalCount"
            binding.tvTargetCpTitle.text = activeCp.name
            binding.tvDistanceMeters.text = "📍 ${state.formattedDistance()}"
            binding.tvBearingDegrees.text = "🧭 ${state.formattedBearing()}"
            binding.tvClueHint.text = "Clue: ${activeCp.clue}"

            // Geofence status badge
            val visualState = state.activeGeofenceVisualState
            when (visualState?.state) {
                GeofenceState.INSIDE -> {
                    binding.tvGeofenceStateBadge.text = "INSIDE RADIUS"
                    binding.tvGeofenceStateBadge.setBackgroundResource(R.drawable.bg_badge_emerald)
                    binding.tvGeofenceStateBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary_emerald))
                }
                GeofenceState.OUTSIDE -> {
                    binding.tvGeofenceStateBadge.text = "OUTSIDE RADIUS"
                    binding.tvGeofenceStateBadge.setBackgroundResource(R.drawable.bg_badge_cyan)
                    binding.tvGeofenceStateBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary_emerald))
                }
                else -> {
                    binding.tvGeofenceStateBadge.text = "ACQUIRING..."
                    binding.tvGeofenceStateBadge.setBackgroundResource(R.drawable.bg_badge_gold)
                    binding.tvGeofenceStateBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_secondary_amber))
                }
            }

            // Stepper buttons state
            binding.btnPrevWaypoint.isEnabled = state.activeCheckpointIndex > 0
            binding.btnNextWaypoint.isEnabled = state.activeCheckpointIndex < totalCount - 1

            // FAB state & styling
            if (state.isScanEligible) {
                binding.fabEnterScan.text = "⚡ Enter Discovery HUD"
                binding.fabEnterScan.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.primary_emerald)
                )
                binding.fabEnterScan.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.on_primary_emerald)
                )
            } else {
                val dist = state.targetDistanceMeters?.toInt()
                val distText = if (dist != null) " (${dist}m)" else ""
                binding.fabEnterScan.text = "Approach Target$distText"
                binding.fabEnterScan.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.bg_dark_card_elevated)
                )
                binding.fabEnterScan.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.primary_cyan)
                )
            }
        } else {
            binding.cardTargetHud.visibility = View.GONE
        }

        updateMapOverlays(state)
    }

    override fun onMapReady(map: GoogleMap) {
        this.googleMap = map
        if (com.campusquest.BuildConfig.MAPS_API_KEY.contains("Placeholder")) {
            android.util.Log.i("CampusMapFragment", "Running in offline demo mode. Provide a valid MAPS_API_KEY in local.properties for live Google Maps tiles.")
        }
        updateMapOverlays(viewModel.uiState.value)
    }

    private fun updateMapOverlays(state: CampusMapUiState) {
        val map = googleMap ?: return
        map.clear()

        // Draw checkpoint geofence circles and markers
        state.checkpoints.forEachIndexed { index, cp ->
            val position = LatLng(cp.lat, cp.lng)
            val visualState = state.geofenceVisualStates[cp.id]
            val isCurrent = index == state.activeCheckpointIndex

            // Stroke color based on geofence state (M1 styling)
            val strokeColor = when (visualState?.state) {
                GeofenceState.INSIDE -> ContextCompat.getColor(requireContext(), R.color.primary_emerald)
                GeofenceState.OUTSIDE -> ContextCompat.getColor(requireContext(), R.color.primary_cyan)
                else -> ContextCompat.getColor(requireContext(), R.color.secondary_amber)
            }

            // Semi-transparent fill
            val fillColor = when (visualState?.state) {
                GeofenceState.INSIDE -> 0x4400E676
                GeofenceState.OUTSIDE -> 0x2200E5FF
                else -> 0x44FFD600
            }

            map.addCircle(
                CircleOptions()
                    .center(position)
                    .radius(cp.radiusM.toDouble())
                    .strokeColor(strokeColor)
                    .strokeWidth(if (isCurrent) 6f else 3f)
                    .fillColor(fillColor)
            )

            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("${index + 1}. ${cp.name}")
                    .snippet(cp.clue)
            )
        }

        // Draw Player location marker
        val playerLoc = state.playerLocation
        val lat = playerLoc?.smoothedLatitude ?: playerLoc?.location?.latitude
        val lng = playerLoc?.smoothedLongitude ?: playerLoc?.location?.longitude
        if (lat != null && lng != null) {
            val playerPosition = LatLng(lat, lng)
            map.addMarker(
                MarkerOptions()
                    .position(playerPosition)
                    .title("Your Location")
            )
        }

        // Center map on active checkpoint if available
        state.activeCheckpoint?.let { activeCp ->
            val centerPos = LatLng(activeCp.lat, activeCp.lng)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPos, 16.5f))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
