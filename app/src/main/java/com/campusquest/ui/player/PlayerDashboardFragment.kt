package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.campusquest.CampusQuestApplication
import com.campusquest.ui.common.AppProviders
import kotlinx.coroutines.launch
import com.campusquest.R
import com.campusquest.databinding.FragmentPlayerDashboardBinding

class PlayerDashboardFragment : Fragment() {

    private var _binding: FragmentPlayerDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = AppProviders.authRepository.getCurrentUserId()
        if (uid != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    CampusQuestApplication.instance.database.playerProgressDao().observeUserProgress(uid).collect { rows ->
                        val score = rows.sumOf { it.score }
                        val relics = rows.sumOf { it.completedCheckpointIds.size }
                        binding.tvStats.text = getString(R.string.player_progress_stats, score, relics)
                    }
                }
            }
        }

        binding.btnBrowseQuests.setOnClickListener {
            findNavController().navigate(R.id.action_player_to_gamesList)
        }

        binding.btnSwitchCreator.setOnClickListener {
            findNavController().navigate(R.id.action_player_to_creatorDashboard)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
