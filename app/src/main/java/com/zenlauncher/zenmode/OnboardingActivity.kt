package com.zenlauncher.zenmode

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class OnboardingActivity : AppCompatActivity() {

    companion object {
        /** Position of the Google Sign-In page (mandatory). */
        private const val GOOGLE_SIGN_IN_PAGE = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val analyticsTracker = ServiceLocator.analyticsTracker
        analyticsTracker.trackOnboardingStarted()

        val analyticsManager = ServiceLocator.analyticsManager
        val repository = UsageRepository(this, analyticsManager)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = OnboardingAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = true // Allow swiping to freely navigate
        
        val indicator = findViewById<TextView>(R.id.tv_page_indicator)
        
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.pb_header)
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Block forward navigation past the Google Sign-In page if not signed in
                if (position > GOOGLE_SIGN_IN_PAGE && !isUserSignedIn()) {
                    viewPager.setCurrentItem(GOOGLE_SIGN_IN_PAGE, true)
                    Toast.makeText(
                        this@OnboardingActivity,
                        "Please sign in with Google to continue",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                indicator.text = "${position + 1}/7"
                progressBar.progress = when (position) {
                    0 -> 14
                    1 -> 28
                    2 -> 42
                    3 -> 57
                    4 -> 71
                    5 -> 85
                    else -> 100
                }
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

    private fun isUserSignedIn(): Boolean {
        return ServiceLocator.authProvider.isSignedIn()
    }
}


