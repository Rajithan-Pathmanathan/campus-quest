package com.campusquest.story

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

enum class LocationAccessAction { REQUEST_PERMISSION, APP_SETTINGS, ENABLE_SERVICES, READY }

fun locationAccessAction(granted: Boolean, services: Boolean, requested: Boolean, rationale: Boolean): LocationAccessAction = when {
    !granted && requested && !rationale -> LocationAccessAction.APP_SETTINGS
    !granted -> LocationAccessAction.REQUEST_PERMISSION
    !services -> LocationAccessAction.ENABLE_SERVICES
    else -> LocationAccessAction.READY
}

/** Runtime permission and the device location switch are separate Android controls. */
class LocationAccess(private val fragment: Fragment, private val refresh: () -> Unit, private val message: (String) -> Unit) {
    private var checking = false
    private val settings = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { recheck() }
    private val resolution = fragment.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { recheck() }
    private val permissions = fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh()
        if (granted()) enableServices()
        else message("Location access was not allowed. Tap Allow location to try again or open app settings.")
    }
    private fun granted(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(fragment.requireContext(),it)==PackageManager.PERMISSION_GRANTED
    }
    private fun enabled() = LocationManagerCompat.isLocationEnabled(fragment.requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager)
    fun request() {
        val preferences=fragment.requireContext().getSharedPreferences("location_access",Context.MODE_PRIVATE)
        val rationale=fragment.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) || fragment.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
        when(locationAccessAction(granted(),enabled(),preferences.getBoolean("requested",false),rationale)) {
            LocationAccessAction.REQUEST_PERMISSION -> {
                preferences.edit().putBoolean("requested",true).apply()
                permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
            }
            LocationAccessAction.APP_SETTINGS -> MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle("Allow location access")
                .setMessage("Android is no longer showing the permission prompt. Open app settings, choose Permissions, then allow Location. Precise location works best for the checkpoint circles.")
                .setPositiveButton("Open app settings") { _,_ -> settings.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${fragment.requireContext().packageName}"))) }
                .setNegativeButton("Not now",null).show()
            LocationAccessAction.ENABLE_SERVICES -> enableServices()
            LocationAccessAction.READY -> recheck()
        }
    }
    private fun enableServices() {
        if(enabled()) { recheck(); return }
        if(checking)return
        checking=true
        val request=LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000).build()
        ).setAlwaysShow(true).build()
        LocationServices.getSettingsClient(fragment.requireActivity()).checkLocationSettings(request)
            .addOnSuccessListener { checking=false;if(fragment.isAdded && fragment.view!=null)recheck() }
            .addOnFailureListener { error ->
                checking=false
                if(!fragment.isAdded || fragment.view==null)return@addOnFailureListener
                if(error is ResolvableApiException) {
                    runCatching { resolution.launch(IntentSenderRequest.Builder(error.resolution).build()) }
                        .onFailure { openLocationSettings() }
                } else openLocationSettings()
            }
    }
    private fun openLocationSettings() {
        message("Turn on location services, then return to the game.")
        runCatching { settings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .onFailure { message("Open your phone settings and switch on Location, then return to the game.") }
    }
    private fun recheck() {
        refresh()
        when {
            !granted() -> message("Allow location permission in app settings to continue.")
            !enabled() -> message("Location services are still off. Tap Allow location to switch them on.")
            else -> message("Location access is ready. Waiting for an accurate GPS position.")
        }
    }
}
