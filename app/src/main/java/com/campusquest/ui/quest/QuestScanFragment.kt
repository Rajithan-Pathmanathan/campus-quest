package com.campusquest.ui.quest

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.DialogClueRevealBinding
import com.campusquest.databinding.FragmentQuestScanBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Checkpoint scan screen. Draws [QuestScanUiState] only: M3 owns the sensors, the ViewModel owns the
 * state machine. Players see story words, never raw numbers.
 */
class QuestScanFragment : Fragment() {

    private var _binding: FragmentQuestScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuestScanViewModel by viewModels { viewModelFactoryOverride ?: QuestScanViewModel.Factory() }

    private var haptics: HeartbeatHaptics? = null
    private var revealDialog: AlertDialog? = null
    private var leftScreen = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
        if (!granted) com.campusquest.ui.common.PermissionNotice.show(this, com.campusquest.ui.common.AppPermission.LOCATION)
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
        haptics = HeartbeatHaptics(requireContext(), viewLifecycleOwner.lifecycleScope)
        binding.ivBack.setOnClickListener { findNavController().navigateUp() }

        // No default ids: a missing argument shows "Checkpoint unavailable" instead of a made-up checkpoint.
        viewModel.load(
            gameId = arguments?.getString("gameId").orEmpty(),
            checkpointId = arguments?.getString("checkpointId").orEmpty()
        )

        binding.btnPrimary.setOnClickListener {
            when (viewModel.uiState.value.primaryAction) {
                ScanAction.BEGIN_SCAN -> viewModel.beginScan()
                ScanAction.GRANT_LOCATION -> requestLocationPermission()
                ScanAction.RETRY -> viewModel.retry()
                ScanAction.BACK -> findNavController().navigateUp()
                ScanAction.NONE -> Unit
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestLocationPermission()
        viewModel.onScreenStarted()
        _binding?.trailCompass?.start()
    }

    override fun onStop() {
        super.onStop()
        haptics?.stop()
        _binding?.trailCompass?.stop()
        viewModel.onScreenStopped()
    }

    override fun onDestroyView() {
        revealDialog?.dismiss()
        revealDialog = null
        haptics?.stop()
        haptics = null
        _binding = null
        super.onDestroyView()
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun render(state: QuestScanUiState) {
        val b = _binding ?: return
        val location = com.campusquest.CampusQuestApplication.instance.locationRepository.locationState.value
        b.trailCompass.update(state.navigationBearing, state.navigationDistance, location.smoothedLatitude, location.smoothedLongitude)
        b.tvCheckpointName.text = state.checkpointName
        b.tvHeadline.text = state.headline
        b.progressLoading.visibility = if (state.scanState == ScanState.LOADING) View.VISIBLE else View.GONE

        // The compass already prints the distance once it knows it; the words cover the time before that.
        b.tvDistance.setTextOrHide(if (state.navigationDistance == null) state.distanceText else null)
        b.tvWarmth.setTextOrHide(null)
        bindSignals(b, state)
        b.tvLightHint.setTextOrHide(state.lightHint)
        b.tvTrialHint.setTextOrHide(state.trialHint)
        b.tvShieldHint.setTextOrHide(state.shieldHint)
        // Proximity fallback for phones whose proximity sensor does nothing: press and hold the button.
        b.btnShield.visibility = if (state.shieldHint != null) View.VISIBLE else View.GONE
        b.btnShield.setOnLongClickListener { viewModel.onShieldHeld(); true }
        b.tvShieldHint.setOnLongClickListener { viewModel.onShieldHeld(); true }
        b.tvErrorMessage.setTextOrHide(state.error?.message)
        // The tip card only appears while it has something to say.
        b.panelHints.visibility =
            if (listOf(b.tvTrialHint, b.tvLightHint, b.tvShieldHint, b.tvErrorMessage).any { it.visibility == View.VISIBLE })
                View.VISIBLE else View.GONE

        haptics?.update(state.heartbeatIntervalMs)

        val label = when (state.primaryAction) {
            ScanAction.BEGIN_SCAN -> getString(R.string.scan_action_begin)
            ScanAction.GRANT_LOCATION -> getString(R.string.scan_action_allow_location)
            ScanAction.RETRY -> getString(R.string.scan_action_retry)
            ScanAction.BACK -> getString(R.string.scan_action_back)
            ScanAction.NONE -> null
        }
        b.btnPrimary.visibility = if (label == null) View.GONE else View.VISIBLE
        b.btnPrimary.text = label

        when (state.scanState) {
            ScanState.REVEALING -> state.reveal?.let(::showReveal)
            ScanState.COMPLETED -> leaveAfterCompletion(state)
            else -> Unit
        }
    }

    /** Next chapter: back to the map (which picks the next checkpoint). Last chapter: this game's leaderboard. */
    private fun leaveAfterCompletion(state: QuestScanUiState) {
        if (leftScreen) return
        leftScreen = true
        val nav = findNavController()
        if (state.isFinalCheckpoint) {
            nav.navigate(
                R.id.leaderboardFragment,
                Bundle().apply { putString("gameId", state.gameId) },
                NavOptions.Builder().setPopUpTo(R.id.questScanFragment, true).build()
            )
        } else {
            nav.navigateUp()
        }
    }

    /** Shown from state, so it survives rotation; only one dialog at a time. */
    private fun showReveal(reveal: RevealContent) {
        if (revealDialog?.isShowing == true) return
        val dialogBinding = DialogClueRevealBinding.inflate(layoutInflater)
        dialogBinding.tvDialogCpName.text = reveal.checkpointName
        dialogBinding.tvDialogRarity.text = reveal.rarity
        dialogBinding.tvDialogClue.setTextOrHide(reveal.clue)
        dialogBinding.tvDialogLore.setTextOrHide(reveal.lore)
        dialogBinding.tvDialogPoints.text = getString(R.string.scan_reveal_points, reveal.points)
        dialogBinding.btnDialogNext.text =
            getString(if (reveal.isFinalCheckpoint) R.string.scan_reveal_complete_quest else R.string.scan_reveal_next_chapter)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.btnDialogNext.setOnClickListener {
            dialog.dismiss()
            viewModel.onRevealDismissed()
        }
        revealDialog = dialog
        dialog.show()
    }

    /** GPS, light and motion as a word and a colour: gold when matched, cream when close, muted while waiting. */
    private fun bindSignals(b: FragmentQuestScanBinding, state: QuestScanUiState) {
        b.rowSignals.visibility = if (state.showSignals) View.VISIBLE else View.GONE
        fun paint(icon: android.widget.ImageView, text: android.widget.TextView, chip: SignalChip) {
            val color = androidx.core.content.ContextCompat.getColor(
                requireContext(),
                when (chip.level) {
                    SignalLevel.MATCHED -> R.color.secondary_amber
                    SignalLevel.CLOSE -> R.color.on_primary_emerald
                    SignalLevel.WAITING -> R.color.text_hint
                }
            )
            text.text = chip.status
            text.setTextColor(color)
            icon.setColorFilter(color)
        }
        paint(b.ivGps, b.tvGpsState, state.gps)
        paint(b.ivLight, b.tvLightState, state.light)
        paint(b.ivMotion, b.tvMotionState, state.motion)
    }

    private fun android.widget.TextView.setTextOrHide(value: String?) {
        text = value
        visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    companion object {
        /** Instrumented tests inject a ViewModel built from fake repositories here. */
        @VisibleForTesting
        var viewModelFactoryOverride: ViewModelProvider.Factory? = null
    }
}
