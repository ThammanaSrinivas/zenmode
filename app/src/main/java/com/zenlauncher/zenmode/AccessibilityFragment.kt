package com.zenlauncher.zenmode

import android.content.Context
import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class AccessibilityFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_accessibility, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<ImageView>(R.id.btn_finish_setup).setOnClickListener {
            // Mark onboarding as complete and finish
            val analyticsManager = ServiceLocator.analyticsManager
            val repository = UsageRepository(requireContext(), analyticsManager)
            repository.setOnboardingComplete(true)

            // Clear saved onboarding page position
            repository.clearOnboardingCurrentPage()

            // Analytics
            val analyticsTracker = ServiceLocator.analyticsTracker
            analyticsTracker.trackOnboardingCompleted()
            
            // Navigate to MainActivity
            val mainIntent = Intent(requireContext(), com.zenlauncher.zenmode.MainActivity::class.java)
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(mainIntent)
            requireActivity().finish()
        }
    }
}
