package com.zenlauncher.zenmode

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class DefaultLauncherFragment : Fragment() {

    private var hasOpenedSettings = false

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // User returned from settings — check if ZenMode is now the default launcher
        if (isDefaultLauncher()) {
            navigateToNextPage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_default_launcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btn_set_default_launcher).setOnClickListener {
            hasOpenedSettings = true
            // Open Android's home app settings so the user can pick ZenMode
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            settingsLauncher.launch(intent)
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = requireContext().packageManager.resolveActivity(
            intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == requireContext().packageName
    }

    override fun onResume() {
        super.onResume()
        // When the activity is re-created after setting ZenMode as default launcher,
        // auto-advance to the next page
        if (isDefaultLauncher()) {
            navigateToNextPage()
        }
    }

    private fun navigateToNextPage() {
        val viewPager = activity?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        if (viewPager != null) {
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }
}
