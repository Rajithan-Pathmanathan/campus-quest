package com.campusquest.ui.creator

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.campusquest.R
import com.campusquest.ui.common.AppProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * "Delete quest" for the quest's creator (guest or email account). The repository refuses
 * anyone else, and the Firestore rules refuse them again in the cloud.
 */
object QuestDeletion {

    fun isCreator(creatorId: String?): Boolean {
        val uid = AppProviders.authRepository.getCurrentUserId()
        return uid != null && creatorId == uid
    }

    fun confirmAndDelete(fragment: Fragment, gameId: String, onDeleted: () -> Unit) {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.quest_delete_title)
            .setMessage(R.string.quest_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.quest_delete_confirm) { _, _ ->
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppProviders.gameRepository.deleteGame(gameId)
                    if (!fragment.isAdded) return@launch
                    result.onSuccess {
                        Toast.makeText(fragment.requireContext(), R.string.quest_deleted, Toast.LENGTH_SHORT).show()
                        onDeleted()
                    }.onFailure { e ->
                        Toast.makeText(fragment.requireContext(), e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }
}
