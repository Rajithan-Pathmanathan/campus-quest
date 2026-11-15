package com.campusquest.ui.quest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.campusquest.R
import com.campusquest.ui.common.AppPermission
import com.campusquest.ui.common.PermissionNotice
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Asks for what the quest geofence needs, then runs the callback whatever the answers are
 * (the in-app zones work without them):
 * 1. location (+ notifications on Android 13+),
 * 2. once only, after a short explanation: background location ("Allow all the time"),
 *    which Android 10+ requires before geofences can be registered.
 *
 * Create it as a Fragment property (it registers activity-result launchers).
 */
class GeofencePermissionFlow(private val fragment: Fragment) {

    private var then: (() -> Unit)? = null

    private val foreground = fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val declined = buildList {
            if (!granted(Manifest.permission.ACCESS_FINE_LOCATION) && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) add(AppPermission.LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                grants[Manifest.permission.POST_NOTIFICATIONS] == false) add(AppPermission.NOTIFICATIONS)
        }
        PermissionNotice.show(fragment, declined) { askBackgroundOnce() }
    }
    private val background = fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) finish() else PermissionNotice.show(fragment, AppPermission.BACKGROUND_LOCATION) { finish() }
    }

    fun run(onDone: () -> Unit) {
        then = onDone
        val missing = foregroundPermissions().filterNot { granted(it) }
        if (missing.isEmpty()) askBackgroundOnce() else foreground.launch(missing.toTypedArray())
    }

    private fun askBackgroundOnce() {
        val context = fragment.context ?: return finish()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            !granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ||
            prefs.getBoolean(KEY_ASKED, false)
        ) return finish()

        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.geofence_permission_title)
            .setMessage(R.string.geofence_permission_message)
            .setPositiveButton(R.string.geofence_permission_allow) { _, _ ->
                background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton(R.string.geofence_permission_not_now) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun finish() {
        val callback = then
        then = null
        callback?.invoke()
    }

    private fun granted(permission: String): Boolean {
        val context = fragment.context ?: return false
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun foregroundPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val PREFS = "geofence_permissions"
        const val KEY_ASKED = "background_asked"
    }
}
