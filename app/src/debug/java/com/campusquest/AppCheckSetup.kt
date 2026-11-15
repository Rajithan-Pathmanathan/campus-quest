package com.campusquest

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the App Check debug provider. On first run Logcat prints a debug token
 * ("Enter this debug secret into the allow list"); add it in Firebase Console -> App Check
 * -> Apps -> Manage debug tokens before enforcing App Check.
 */
object AppCheckSetup {
    fun install() {
        runCatching { FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance()) }
    }
}
