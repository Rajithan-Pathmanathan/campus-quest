package com.campusquest.ui.map

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

    /** The player's position marker, moved as M3's location updates arrive. */
    private var playerMarker: Marker? = null

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
        showPlayer()

        // Opens the scan for the player's next unfound checkpoint (dynamic, by the game's own order).
        binding.fabEnterScan.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val nextId = try {
                    val repo = AppProviders.gameRepository
                    val completed = repo.observePlayerProgress(gameId, "current_user").first()
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
                findNavController().navigate(R.id.action_campusMap_to_questScan, bundle)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
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
            val (checkpoints, completed) = try {
                val repo = AppProviders.gameRepository
                // Same player id the scan screen uses until real user ids are wired in (M6).
                repo.getGameCheckpoints(gameId) to
                    repo.observePlayerProgress(gameId, "current_user").first()?.completedCheckpointIds.orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@launch
            }
            val map = _binding?.mapView ?: return@launch
            checkpoints.sortedBy { it.order }.forEach { cp -> addCheckpoint(cp, found = cp.id in completed) }
            val target = NextCheckpointResolver.next(checkpoints, completed) ?: checkpoints.lastOrNull()
            target?.let { map.controller.setCenter(GeoPoint(it.lat, it.lng)) }
            map.invalidate()
        }
    }

    private fun addCheckpoint(cp: Checkpoint, found: Boolean) {
        val map = _binding?.mapView ?: return
        val position = GeoPoint(cp.lat, cp.lng)
        val color = ContextCompat.getColor(requireContext(), if (found) R.color.primary_emerald else R.color.secondary_amber)
        map.overlays.add(Polygon(map).apply {
            points = Polygon.pointsAsCircle(position, cp.radiusM.toDouble())
            outlinePaint.color = color
            outlinePaint.strokeWidth = 4f
            fillPaint.color = (color and 0x00FFFFFF) or 0x33000000
            setOnClickListener { _, _, _ -> false } // let taps reach the marker
        })
        map.overlays.add(Marker(map).apply {
            this.position = position
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = (if (found) "✅ " else "${cp.order}. ") + cp.name
            snippet = cp.clue
        })
    }

    /** Shows the player's latest position from M3's location repository (tracking is started by the scan flow). */
    private fun showPlayer() {
        val locationRepository = (requireActivity().application as CampusQuestApplication).locationRepository
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationRepository.locationState.collect { loc ->
                    val lat = loc.smoothedLatitude ?: return@collect
                    val lng = loc.smoothedLongitude ?: return@collect
                    val map = _binding?.mapView ?: return@collect
                    val marker = playerMarker ?: Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "You"
                        map.overlays.add(this)
                        playerMarker = this
                    }
                    marker.position = GeoPoint(lat, lng)
                    map.invalidate()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerMarker = null
        _binding = null
    }
}
