package com.campusquest.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentOnboardingBinding
import com.campusquest.ui.common.MainActivity

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private var page = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = savedInstanceState?.getInt(STATE_PAGE) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (preferences().getBoolean(KEY_COMPLETE, false)) {
            binding.root.isVisible = false
            openApp()
            return
        }

        render()
        binding.btnOnboardingAction.setOnClickListener {
            if (page == 0) {
                page = 1
                render()
            } else {
                preferences().edit().putBoolean(KEY_COMPLETE, true).apply()
                openApp()
                binding.root.post { (activity as? MainActivity)?.handlePendingNotification() }
            }
        }
    }

    private fun render() {
        val secondPage = page == 1
        binding.ivOnboardingArtwork.setImageResource(
            if (secondPage) R.drawable.onboarding_light_the_path else R.drawable.onboarding_welcome
        )
        binding.ivOnboardingArtwork.contentDescription = getString(
            if (secondPage) R.string.onboarding_page_two_description else R.string.onboarding_page_one_description
        )
        binding.btnOnboardingAction.contentDescription = getString(
            if (secondPage) R.string.onboarding_get_started else R.string.onboarding_next
        )
    }

    private fun openApp() {
        if (findNavController().currentDestination?.id == R.id.onboardingFragment) {
            findNavController().navigate(R.id.action_onboarding_to_login)
        }
    }

    private fun preferences() = requireContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_PAGE, page)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val PREFERENCES = "onboarding"
        const val KEY_COMPLETE = "complete"
        const val STATE_PAGE = "page"
    }
}
