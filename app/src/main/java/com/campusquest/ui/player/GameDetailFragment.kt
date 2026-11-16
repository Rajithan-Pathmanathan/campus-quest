package com.campusquest.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentGameDetailBinding
import com.campusquest.domain.model.GameStatus
import kotlinx.coroutines.launch

class GameDetailFragment : Fragment() {

    private var _binding: FragmentGameDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameDetailViewModel by viewModels()
    private val gameId: String get() = arguments?.getString("gameId").orEmpty()
    private val geofencePermissions = com.campusquest.ui.quest.GeofencePermissionFlow(this)

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

        viewModel.load(gameId)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnRetry.setOnClickListener { viewModel.load(gameId, force = true) }
        binding.btnJoin.setOnClickListener { viewModel.join() }

        binding.btnStartQuest.setOnClickListener {
            // M3: permissions for the quest geofence, then a real geofence around the next place.
            geofencePermissions.run { startQuest() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val app = com.campusquest.CampusQuestApplication.instance
            if(app.database.storyDao().config(gameId) != null && app.database.gameDao().getGameById(gameId)?.creatorId == com.campusquest.ui.common.AppProviders.authRepository.getCurrentUserId()) {
                val button = com.google.android.material.button.MaterialButton(requireContext()).apply {
                    text = "View / save repair card"
                    setOnClickListener {findNavController().navigate(R.id.storySetupFragment,Bundle().apply {putString("gameId",gameId)})}
                }
                (binding.btnJoin.parent as android.view.ViewGroup).addView(button)
            }
        }
        binding.btnDeleteQuest.setOnClickListener {
            com.campusquest.ui.creator.QuestDeletion.confirmAndDelete(this, gameId) { findNavController().navigateUp() }
        }
        binding.btnViewLeaderboard.setOnClickListener {
            findNavController().navigate(
                R.id.action_gameDetail_to_leaderboard,
                Bundle().apply { putString("gameId", gameId) }
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun startQuest() {
        if (_binding == null) return
        com.campusquest.CampusQuestApplication.instance.questGeofences.activate(gameId, force = true)
        viewLifecycleOwner.lifecycleScope.launch {
            val story = com.campusquest.CampusQuestApplication.instance.database.storyDao().config(gameId)
            findNavController().navigate(if (story != null) R.id.storyGameFragment else R.id.campusMapFragment,
                Bundle().apply {putString("gameId",gameId)})
        }
    }

    private fun render(state: GameDetailUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.layoutError.isVisible = !state.isLoading && state.error != null
        binding.layoutContent.isVisible = !state.isLoading && state.game != null
        binding.tvErrorMessage.text = state.error

        state.game?.let { game ->
            binding.tvTitle.text = game.title
            binding.tvCreator.text = getString(R.string.quest_details_creator, game.creatorName)
            binding.tvStatus.text = game.status.name
            binding.tvCheckpointCount.text = resources.getQuantityString(
                R.plurals.quest_details_checkpoints,
                game.checkpointCount,
                game.checkpointCount
            )
            binding.tvDescription.text = game.description

            val joined = state.joinStatus == JoinStatus.JOINED
            val joining = state.joinStatus == JoinStatus.JOINING
            val canJoin = game.status == GameStatus.PUBLISHED

            binding.btnJoin.isVisible = !joined
            binding.btnJoin.isEnabled = canJoin && !joining
            binding.btnJoin.text = getString(
                when {
                    !canJoin -> R.string.join_not_available
                    joining -> R.string.join_joining
                    state.joinStatus == JoinStatus.FAILED -> R.string.join_try_again
                    else -> R.string.join_quest
                }
            )
            binding.btnStartQuest.isVisible = joined
            // Only the person who created the quest (guest or email account) sees "Delete quest".
            binding.btnDeleteQuest.isVisible = com.campusquest.ui.creator.QuestDeletion.isCreator(game.creatorId)
        }

        binding.tvJoinError.isVisible = state.joinError != null
        binding.tvJoinError.text = state.joinError
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
