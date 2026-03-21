package com.zenlauncher.zenmode

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

class UsagePermissionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                com.zenlauncher.zenmode.ui.theme.ZenTheme {
                    com.zenlauncher.zenmode.ui.screens.UsageAccessPermissionScreen(
                        onGrantAccessClick = { handleGrantAccess() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Compose view handles its own interactions
    }
    
    private fun handleGrantAccess() {
        if (hasUsageStatsPermission()) {
            // Analytics
            com.zenlauncher.zenmode.coreapi.services.ServiceLocator.analyticsTracker.trackPermissionsGranted("usage_access")

            // Advance to next page
            val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
            viewPager.currentItem = viewPager.currentItem + 1
        } else {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }
    
    override fun onResume() {
        super.onResume()
        // If user comes back and permission is granted, maybe auto-advance
        if (hasUsageStatsPermission()) {
            val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)
            if (viewPager != null && viewPager.adapter != null && viewPager.currentItem < (viewPager.adapter!!.itemCount - 1)) {
                 viewPager.currentItem = viewPager.currentItem + 1
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = requireContext().getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                requireContext().packageName
            )
        } else {
             appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                requireContext().packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}
