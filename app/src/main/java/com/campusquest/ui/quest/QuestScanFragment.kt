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
import androidx.lifecycle.Lifecycle
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

    private val viewModel: QuestScanViewModel by viewModels { QuestScanViewModel.Factory() }

    private var haptics: HeartbeatHaptics? = null
    private var revealDialog: AlertDialog? = null
    private var leftScreen = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
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
    }

    override fun onStop() {
        super.onStop()
        haptics?.stop()
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
        b.tvCheckpointName.text = state.checkpointName
        b.tvHeadline.text = state.headline
        b.progressLoading.visibility = if (state.scanState == ScanState.LOADING) View.VISIBLE else View.GONE

        b.tvDistance.setTextOrHide(state.distanceText)
        b.tvWarmth.setTextOrHide(state.warmth)
        b.tvLightHint.setTextOrHide(state.lightHint)
        b.tvTrialHint.setTextOrHide(state.trialHint)
        b.tvShieldHint.setTextOrHide(state.shieldHint)
        b.tvErrorMessage.setTextOrHide(state.error?.message)

        val glow = state.glow
        b.pbEmber.visibility = if (glow != null) View.VISIBLE else View.GONE
        if (glow != null) {
            b.pbEmber.progress = glow.ordinal
            b.pbEmber.contentDescription = glow.label
        }

        haptics?.update(state.heartbeatIntervalMs)

        val label = when (state.primaryAction) {
            ScanAction.BEGIN_SCAN -> "Wake the Resonator"
            ScanAction.GRANT_LOCATION -> "Allow location"
            ScanAction.RETRY -> "Try again"
            ScanAction.BACK -> "Back to the quest"
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
        dialogBinding.tvDialogClue.text = reveal.clue
        dialogBinding.tvDialogLore.text = reveal.lore
        revealDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .setPositiveButton(if (reveal.isFinalCheckpoint) "Complete the quest" else "Next chapter") { _, _ ->
                viewModel.onRevealDismissed()
            }
            .show()
    }

    private fun android.widget.TextView.setTextOrHide(value: String?) {
        text = value
        visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
    }
}
