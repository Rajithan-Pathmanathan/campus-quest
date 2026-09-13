package com.campusquest.ui.common

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.campusquest.R
import com.campusquest.databinding.ActivityMainBinding

/**
 * Single Activity Host for Campus Quest.
 * Coordinates Jetpack Navigation, Top App Bar sync, immersive full-screen switching,
 * and FCM push notification deep-link routing.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Top-level destinations without Up arrow
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.loginFragment,
                R.id.playerDashboardFragment,
                R.id.creatorDashboardFragment
            )
        )

        binding.topAppBar.setupWithNavController(navController, appBarConfiguration)

        // Toggle toolbar visibility on immersive screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.questScanFragment -> {
                    binding.appBarLayout.visibility = View.GONE
                }
                else -> {
                    binding.appBarLayout.visibility = View.VISIBLE
                }
            }
        }

        handleDeepLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Check FCM payload extra "gameId"
        val gameId = intent.getStringExtra("gameId")
        if (!gameId.isNullOrBlank()) {
            try {
                val args = bundleOf("gameId" to gameId)
                navController.navigate(R.id.gameDetailFragment, args)
            } catch (e: Exception) {
                // Ignore if nav graph is not ready yet
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
