package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.databinding.FragmentCreatorDashboardBinding
import kotlinx.coroutines.launch
import com.campusquest.ui.common.AppProviders

class CreatorDashboardFragment : Fragment() {

    private var _binding: FragmentCreatorDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreatorGamesViewModel by viewModels {
        CreatorGamesViewModelFactory(
            repository = CampusQuestApplication.instance.gameRepository,
            // TODO: replace with AuthRepository.getCurrentUserId() once we know where to get it
            currentUserId = {AppProviders.authRepository.getCurrentUserId() }
        )
    }

    private val adapter = GameCardAdapter { game ->
        val bundle = Bundle().apply { putString("gameId", game.id) }
        findNavController().navigate(R.id.action_creator_to_checkpointList, bundle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatorDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAuthoredGames.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAuthoredGames.adapter = adapter

        binding.btnCreateNewQuest.setOnClickListener {
            findNavController().navigate(R.id.action_creator_to_createGame)
        }
        binding.btnSwitchPlayer.setOnClickListener {
            findNavController().navigate(R.id.action_creator_to_playerDashboard)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.games)
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onResume() {
        super.onResume()
        viewModel.load()
    }
}