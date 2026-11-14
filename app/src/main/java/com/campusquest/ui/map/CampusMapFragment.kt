package com.campusquest.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentCampusMapBinding
import com.campusquest.ui.common.AppProviders
import com.campusquest.ui.quest.NextCheckpointResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CampusMapFragment : Fragment() {

    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
