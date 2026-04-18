package com.zenlauncher.zenmode

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val analyticsTracker = ServiceLocator.analyticsTracker

        val analyticsManager = ServiceLocator.analyticsManager
        val repository = UsageRepository(this, analyticsManager)

        // Record onboarding start time if not already set
        if (repository.getOnboardingStartTime() == 0L) {
            repository.setOnboardingStartTime(System.currentTimeMillis())
        }

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = OnboardingAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = true // Allow swiping to freely navigate

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Persist the current page so we can restore it if the activity is re-created
                repository.setOnboardingCurrentPage(position)
            }
        })

        // Restore saved page (e.g. after HOME intent re-creates onboarding mid-flow)
        val savedPage = repository.getOnboardingCurrentPage()
        if (savedPage > 0) {
            viewPager.setCurrentItem(savedPage, false)
        }
    }
}


