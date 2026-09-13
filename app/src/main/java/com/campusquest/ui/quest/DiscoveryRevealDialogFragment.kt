package com.campusquest.ui.quest

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.DialogDiscoveryRevealBinding
import kotlinx.coroutines.launch

/**
 * Modal dialog presented upon successful physical multi-sensor resonance and relic discovery.
 */
class DiscoveryRevealDialogFragment : DialogFragment() {

    private var _binding: DialogDiscoveryRevealBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiscoveryRevealViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDiscoveryRevealBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gameId = arguments?.getString("gameId") ?: "GAME_HERITAGE_001"
        val checkpointId = arguments?.getString("checkpointId") ?: "CP_BELL_TOWER"

        viewModel.loadDiscovery(gameId, checkpointId)
        setupButtons(gameId)
        observeUiState()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupButtons(gameId: String) {
        binding.btnContinueNavigation.setOnClickListener {
            dismiss()
            findNavController().navigateUp()
        }

        binding.btnViewLeaderboard.setOnClickListener {
            dismiss()
            val args = bundleOf("gameId" to gameId)
            findNavController().navigate(R.id.leaderboardFragment, args)
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

    private fun renderUiState(state: DiscoveryRevealUiState) {
        binding.tvRevealTitle.text = state.checkpointName
        binding.tvRevealSequenceBadge.text = state.sequenceBadge
        binding.tvRevealPoints.text = "💎 +${state.pointsAwarded} PTS"
        binding.tvRevealLore.text = state.lore

        if (state.isFinalCheckpoint) {
            binding.btnContinueNavigation.text = "🎉 Complete Quest & Return to Map"
        } else {
            binding.btnContinueNavigation.text = "🧭 Continue Quest Navigation"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(gameId: String, checkpointId: String): DiscoveryRevealDialogFragment {
            return DiscoveryRevealDialogFragment().apply {
                arguments = bundleOf(
                    "gameId" to gameId,
                    "checkpointId" to checkpointId
                )
            }
        }
    }
}
