package com.campusquest.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import com.campusquest.domain.model.LocationPermissionState
import kotlinx.coroutines.flow.combine
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.databinding.FragmentCampusMapBinding
import com.campusquest.domain.model.Checkpoint
import com.campusquest.ui.common.AppProviders
import com.campusquest.ui.quest.NextCheckpointResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class CampusMapFragment : Fragment() {

    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!

    private val locationRepository get() = CampusQuestApplication.instance.locationRepository
    /** M3: live geofence state of the target checkpoint (inside / outside, with hysteresis). */
    private val geofenceData = com.campusquest.device.location.MapGeofenceDataProvider()
    private var targetGeofenceState = com.campusquest.domain.model.GeofenceState.UNKNOWN

    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        startMapLocation()
        if (!hasLocationPermission()) com.campusquest.ui.common.PermissionNotice.show(this, com.campusquest.ui.common.AppPermission.LOCATION)
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapCompass?.start()
        if (hasLocationPermission()) startMapLocation()
        else requestLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun startMapLocation() {
        val granted = hasLocationPermission()
        locationRepository.updatePermissionState(if (granted) LocationPermissionState.Granted else LocationPermissionState.Denied)
        if (granted) locationRepository.startTracking()
    }





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

        val gameId = arguments?.getString("gameId").orEmpty()

        setupMap()
        showCheckpoints(gameId)


        // Opens the scan for the player's next unfound checkpoint (dynamic, by the game's own order).
        binding.fabEnterScan.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val nextId = try {
                    val repo = AppProviders.gameRepository
                    val completed = repo.observePlayerProgress(gameId, com.campusquest.ui.common.AppProviders.authRepository.getCurrentUserId().orEmpty()).first()
                        ?.completedCheckpointIds.orEmpty()
                    NextCheckpointResolver.next(repo.getGameCheckpoints(gameId), completed)?.id
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                if (nextId == null) {
                    Toast.makeText(requireContext(), "No chapter is waiting for you here.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val bundle = Bundle().apply {
                    putString("gameId", gameId)
                    putString("checkpointId", nextId)
                }
                val story = CampusQuestApplication.instance.database.storyDao().config(gameId)
                findNavController().navigate(if(story != null) R.id.storyGameFragment else R.id.questScanFragment, bundle)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapCompass?.start()
        if (hasLocationPermission()) startMapLocation()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        _binding?.mapCompass?.stop()
        locationRepository.stopTracking()
        _binding?.mapView?.onPause()
        super.onPause()
    }

    /**
     * OpenStreetMap via osmdroid: free tiles, no API key or billing. OSM's tile policy requires an
     * identifying user agent; tiles are cached in the app's own cache folder (no storage permission).
     */
    private fun setupMap() {
        val ctx = requireContext().applicationContext
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = ctx.cacheDir
            osmdroidTileCache = java.io.File(ctx.cacheDir, "osm-tiles")
        }
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(17.0)
        }
    }

    /** Checkpoint circles and markers (found ones ticked); the camera starts on the next unfound one. */
    private fun showCheckpoints(gameId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val repo = AppProviders.gameRepository
            val checkpoints = try {
                repo.getGameCheckpoints(gameId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                binding.tvTargetCpTitle.setText(R.string.map_load_failed)
                return@launch
            }
            var centered = false
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    repo.observePlayerProgress(gameId, AppProviders.authRepository.getCurrentUserId().orEmpty()),
                    locationRepository.locationState
                ) { progress, location -> progress?.completedCheckpointIds.orEmpty() to location }
                    .collect { (completed, location) ->
                        val map = _binding?.mapView ?: return@collect
                        val state = MapTargetState.from(checkpoints, completed, location)
                        targetGeofenceState = state.checkpoint
                            ?.let { geofenceData.createVisualState(it, location, targetGeofenceState).state }
                            ?: com.campusquest.domain.model.GeofenceState.UNKNOWN
                        val bearing = state.checkpoint?.takeIf { location.hasValidFix && location.servicesEnabled }?.let {
                            com.campusquest.device.location.HaversineDistanceCalculator().calculateBearingDegrees(location.smoothedLatitude!!, location.smoothedLongitude!!, it.lat, it.lng)
                        }
                        binding.mapCompass.update(bearing, state.distanceMeters?.toFloat(), location.smoothedLatitude, location.smoothedLongitude)
                        binding.tvTargetCpTitle.text = state.checkpoint?.let { getString(R.string.map_target, it.name) }
                            ?: getString(R.string.map_no_remaining)
                        binding.tvClueHint.text = state.checkpoint?.let { getString(R.string.map_clue, it.clue) }.orEmpty()
                        binding.tvDistanceMeters.text = when {
                            state.checkpoint == null -> ""
                            location.permissionState == LocationPermissionState.Denied -> getString(R.string.map_permission_needed)
                            state.distanceMeters != null -> getString(R.string.map_distance, state.distanceMeters)
                            else -> getString(R.string.map_waiting_gps)
                        }
                        binding.fabEnterScan.isEnabled = state.checkpoint != null
                        map.overlays.clear()
                        val insideTarget = targetGeofenceState == com.campusquest.domain.model.GeofenceState.INSIDE
                        checkpoints.sortedBy { it.order }.forEach {
                            addCheckpoint(it, it.id in completed, inside = insideTarget && it.id == state.checkpoint?.id)
                        }
                        if (!centered && state.checkpoint != null) {
                            map.controller.setCenter(GeoPoint(state.checkpoint.lat, state.checkpoint.lng))
                            centered = true
                        }
                        if (location.hasValidFix && location.servicesEnabled) {
                            map.overlays.add(Marker(map).apply {
                                position = GeoPoint(location.smoothedLatitude!!, location.smoothedLongitude!!)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = getString(R.string.map_you)
                            })
                        }
                        map.invalidate()
                    }
            }
        }
    }

    /** [inside]: the player is inside this checkpoint's geofence; the circle is highlighted. */
    private fun addCheckpoint(cp: Checkpoint, found: Boolean, inside: Boolean = false) {
        val map = _binding?.mapView ?: return
        val position = GeoPoint(cp.lat, cp.lng)
        val color = ContextCompat.getColor(requireContext(), if (found || inside) R.color.primary_emerald else R.color.secondary_amber)
        map.overlays.add(Polygon(map).apply {
            points = Polygon.pointsAsCircle(position, cp.radiusM.toDouble())
            outlinePaint.color = color
            outlinePaint.strokeWidth = if (inside) 8f else 4f
            fillPaint.color = (color and 0x00FFFFFF) or (if (inside) 0x66000000 else 0x33000000)
            setOnClickListener { _, _, _ -> false } // let taps reach the marker
        })
        map.overlays.add(Marker(map).apply {
            this.position = position
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = (if (found) "✅ " else "${cp.order}. ") + cp.name
            snippet = if (inside) getString(R.string.map_inside_checkpoint) else cp.clue
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
