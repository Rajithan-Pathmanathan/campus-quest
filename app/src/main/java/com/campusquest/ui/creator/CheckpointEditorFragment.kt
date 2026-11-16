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
import com.campusquest.R
import com.campusquest.databinding.FragmentCheckpointEditorBinding
import com.campusquest.domain.model.CheckpointActivity
import com.campusquest.ui.creator.validation.CreatorLimits
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.campusquest.domain.model.LocationPermissionState
import com.campusquest.domain.model.LocationState
import com.campusquest.ui.common.AppPermission
import com.campusquest.ui.common.PermissionNotice

class CheckpointEditorFragment : Fragment() {

    private var _binding: FragmentCheckpointEditorBinding? = null
    private val binding get() = _binding!!
    private var pin: Marker? = null
    private var youMarker: Marker? = null
    /** The map starts on the creator's real position (once), unless the checkpoint already has one. */
    private var centeredOnUser = false
    private var lastUserLocation: GeoPoint? = null
    private val locationRepository get() = CampusQuestApplication.instance.locationRepository

    /** Permission + the phone's Location switch (asks to turn it on); same helper as the story screens. */
    private val locationAccess = com.campusquest.story.LocationAccess(this, { startLocation() }) { message ->
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

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

        binding.actMotion.setupDropdown(CheckpointActivity.entries.map { it.label }) { pick ->
            viewModel.update { it.copy(motionType = CheckpointActivity.fromLabel(pick).name) }
        }
        binding.actRarity.setupDropdown(CreatorLimits.RARITIES) { pick ->
            viewModel.update { it.copy(rarity = pick) }
        }

        binding.btnSaveCheckpoint.setOnClickListener { viewModel.save() }
        setupMapPicker()
        binding.btnUseMyLocation.setOnClickListener {
            val here = lastUserLocation
            if (here != null) setLocation(here)
            else {
                // No position yet: make sure permission is granted and Location is switched on.
                Toast.makeText(requireContext(), R.string.checkpoint_waiting_location, Toast.LENGTH_SHORT).show()
                locationAccess.request()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationRepository.locationState.collect { showUser(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    binding.etCpName.setIfDifferent(s.name)
                    binding.etLat.setIfDifferent(s.lat)
                    binding.etLng.setIfDifferent(s.lng)
                    showPin(s.lat, s.lng)
                    binding.etRadius.setIfDifferent(s.radiusM)
                    binding.etMinLux.setIfDifferent(s.minLux)
                    binding.etMaxLux.setIfDifferent(s.maxLux)
                    binding.etClue.setIfDifferent(s.clue)
                    binding.etLore.setIfDifferent(s.lore)
                    binding.etOrder.setIfDifferent(s.order)
                    binding.actMotion.setText(CheckpointActivity.fromStored(s.motionType,s.order.toIntOrNull() ?: 1).label, false)
                    binding.actRarity.setText(s.rarity, false)

                    binding.tilCpName.error = s.errors["name"]
                    binding.tilLat.error = s.errors["lat"]
                    binding.tilLng.error = s.errors["lng"]
                    binding.tilRadius.error = s.errors["radius"]
                    binding.tilMinLux.error = s.errors["lux"]
                    binding.tilClue.error = s.errors["clue"]
                    binding.tilOrder.error = s.errors["order"]
                    binding.tilMotion.error = s.errors["activity"]
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

    /** Second way to set the location: tap the map (or drag the pin). It fills the same lat/lng fields. */
    private fun setupMapPicker() {
        val ctx = requireContext().applicationContext
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = ctx.cacheDir
            osmdroidTileCache = File(ctx.cacheDir, "osm-tiles")
        }
        binding.mapPicker.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(17.0)
            controller.setCenter(CAMPUS_CENTER)
            // Let the map pan and zoom instead of the page scrolling.
            setOnTouchListener { v, _ -> v.parent.requestDisallowInterceptTouchEvent(true); false }
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean { setLocation(p); return true }
                override fun longPressHelper(p: GeoPoint): Boolean = false
            }))
        }
    }

    /** Writing the fields triggers the existing text watchers, so the ViewModel stays the single source of truth. */
    private fun setLocation(p: GeoPoint) {
        binding.etLat.setText(String.format(Locale.US, "%.6f", p.latitude))
        binding.etLng.setText(String.format(Locale.US, "%.6f", p.longitude))
    }

    private fun showPin(lat: String, lng: String) {
        val la = lat.trim().toDoubleOrNull()?.takeIf { it in -90.0..90.0 } ?: return
        val lo = lng.trim().toDoubleOrNull()?.takeIf { it in -180.0..180.0 } ?: return
        val map = binding.mapPicker
        val point = GeoPoint(la, lo)
        val marker = pin ?: Marker(map).also { m ->
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            m.isDraggable = true
            m.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) = Unit
                override fun onMarkerDragStart(marker: Marker) = Unit
                override fun onMarkerDragEnd(marker: Marker) = setLocation(marker.position)
            })
            map.overlays.add(m)
            pin = m
            map.controller.setCenter(point)
        }
        if (marker.position == point) return
        marker.position = point
        if (map.width > 0 && !map.boundingBox.contains(point)) map.controller.animateTo(point)
        map.invalidate()
    }

    override fun onStart() {
        super.onStart()
        if (hasLocationPermission()) startLocation() else locationAccess.request()
    }

    override fun onStop() {
        locationRepository.stopTracking()
        super.onStop()
    }

    private fun hasLocationPermission() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }

    private fun startLocation() {
        val granted = hasLocationPermission()
        locationRepository.updatePermissionState(if (granted) LocationPermissionState.Granted else LocationPermissionState.Denied)
        if (granted) locationRepository.startTracking()
    }

    /** "You are here" marker; the first fix also moves the map to the creator's area (if no pin is set yet). */
    private fun showUser(location: LocationState) {
        val map = _binding?.mapPicker ?: return
        // Only a fix from the current session (tracking restarts with no position).
        if (!location.isUpdating) return
        val lat = location.smoothedLatitude ?: return
        val lng = location.smoothedLongitude ?: return
        val here = GeoPoint(lat, lng)
        lastUserLocation = here
        val marker = youMarker ?: Marker(map).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.title = getString(R.string.map_you)
            it.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.presence_online)
            map.overlays.add(it)
            youMarker = it
        }
        marker.position = here
        if (!centeredOnUser) {
            centeredOnUser = true
            if (pin == null) map.controller.animateTo(here)
        }
        map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapPicker?.onResume()
    }

    override fun onPause() {
        _binding?.mapPicker?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pin = null
        youMarker = null
        _binding = null
    }

    private companion object {
        /** Shown only until the creator's own location arrives (or if location is off). */
        val CAMPUS_CENTER = GeoPoint(6.9743, 79.9160)
    }
}
