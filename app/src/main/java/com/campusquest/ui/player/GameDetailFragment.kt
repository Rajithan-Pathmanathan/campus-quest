package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.data.mock.MockDataCatalog
import com.campusquest.databinding.FragmentGameDetailBinding

class GameDetailFragment : Fragment() {

    private var _binding: FragmentGameDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gameId = arguments?.getString("gameId") ?: "GAME_HERITAGE_001"
        val game = MockDataCatalog.allSampleGames.find { it.id == gameId } ?: MockDataCatalog.sampleGame1

        binding.tvTitle.text = game.title
        binding.tvCreator.text = "Created by: ${game.creatorName}"
        binding.tvDescription.text = game.description

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnStartQuest.setOnClickListener {
            val bundle = Bundle().apply {
                putString("gameId", gameId)
            }
            findNavController().navigate(R.id.action_gameDetail_to_campusMap, bundle)
        }

        binding.btnViewLeaderboard.setOnClickListener {
            val bundle = Bundle().apply {
                putString("gameId", gameId)
            }
            findNavController().navigate(R.id.action_gameDetail_to_leaderboard, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
