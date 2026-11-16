package com.campusquest.ui.common

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.campusquest.R
import com.campusquest.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        /** Same key that CampusQuestMessagingService puts in the notification tap intent. */
        const val EXTRA_GAME_ID = "EXTRA_GAME_ID"
        private val MAIN_DESTINATIONS = setOf(
            R.id.playerDashboardFragment,
            R.id.gamesListFragment,
            R.id.leaderboardFragment,
            R.id.profileFragment
        )
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = destination.id in MAIN_DESTINATIONS
        }

        // Observe global countdown timer state
        lifecycleScope.launch {
            com.campusquest.domain.timer.GameTimerManager.timerState.collect { state ->
                when (state) {
                    is com.campusquest.domain.timer.GameTimerManager.TimerState.Active -> {
                        binding.tvGlobalGameTimer.isVisible = true
                        binding.tvGlobalGameTimer.text = state.formattedTime
                    }
                    is com.campusquest.domain.timer.GameTimerManager.TimerState.Inactive -> {
                        binding.tvGlobalGameTimer.isVisible = false
                    }
                }
            }
        }

        // Handle the notification only on a fresh launch, not after screen rotation.
        if (savedInstanceState == null) {
            binding.root.post { handleNotificationIntent(intent) }
        }
    }

    // App already open (or in the background) and a notification is tapped.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(tapIntent: Intent?) {
        val data = tapIntent ?: return
        val gameId = data.getStringExtra(EXTRA_GAME_ID)?.trim()
        if (gameId.isNullOrEmpty()) return
        if (navController.currentDestination?.id in setOf(R.id.splashFragment, R.id.onboardingFragment)) return

        // Consume the extra so it is not handled twice.
        data.removeExtra(EXTRA_GAME_ID)

        runCatching {
            // On a cold start we are still on the login screen: go to the dashboard first,
            // so Back from the quest details leads to the dashboard.
            if (navController.currentDestination?.id == R.id.loginFragment) {
                navController.navigate(R.id.action_login_to_playerDashboard)
            }
            navController.navigate(
                R.id.gameDetailFragment,
                Bundle().apply { putString("gameId", gameId) }
            )
        }
    }

    internal fun handlePendingNotification() = handleNotificationIntent(intent)

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
