package com.campusquest.ui.common

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.campusquest.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Permissions the game asks for. Phone sensors (light, motion, proximity) need no permission. */
enum class AppPermission(val title: Int, val impact: Int, val howTo: Int) {
    LOCATION(R.string.perm_location_title, R.string.perm_location_impact, R.string.perm_location_how),
    CAMERA(R.string.perm_camera_title, R.string.perm_camera_impact, R.string.perm_camera_how),
    NOTIFICATIONS(R.string.perm_notifications_title, R.string.perm_notifications_impact, R.string.perm_notifications_how),
    BACKGROUND_LOCATION(R.string.perm_background_title, R.string.perm_background_impact, R.string.perm_background_how)
}

/**
 * Formal notice shown after the player declines a permission: what stops working and
 * exactly how to enable it, with a button to the app's settings page.
 * Each permission is explained at most once per app session, so screens that re-ask
 * on every visit don't repeat the dialog.
 */
object PermissionNotice {

    private val explained = mutableSetOf<AppPermission>()

    fun show(fragment: Fragment, declined: Collection<AppPermission>, onContinue: () -> Unit = {}) {
        val context = fragment.context
        val toExplain = declined.filter { it !in explained }
        if (context == null || !fragment.isAdded || toExplain.isEmpty()) return onContinue()
        explained += toExplain

        val title = if (toExplain.size == 1) context.getString(toExplain[0].title)
        else context.getString(R.string.perm_several_title)
        val message = toExplain.joinToString("\n\n") { p ->
            val heading = if (toExplain.size == 1) "" else context.getString(p.title) + "\n"
            heading + context.getString(p.impact) + "\n" + context.getString(R.string.perm_how_prefix, context.getString(p.howTo))
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.perm_open_settings) { _, _ ->
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                onContinue()
            }
            .setNegativeButton(R.string.perm_continue_without) { _, _ -> onContinue() }
            .show()
    }

    fun show(fragment: Fragment, declined: AppPermission, onContinue: () -> Unit = {}) =
        show(fragment, listOf(declined), onContinue)
}
