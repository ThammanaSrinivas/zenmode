package com.zenlauncher.zenmode

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.zenlauncher.zenmode.ui.screens.AccessibilityServiceFragment
import com.zenlauncher.zenmode.ui.screens.DefaultLauncherFragment
import com.zenlauncher.zenmode.ui.screens.SystemOverlayPermissionFragment
import com.zenlauncher.zenmode.ui.screens.UsageAccessPermissionFragment
import com.zenlauncher.zenmode.ui.screens.ZenShoutoutFragment

class OnboardingAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IntroFragment()
            1 -> ZenShoutoutFragment()
            2 -> UsageAccessPermissionFragment()
            3 -> SystemOverlayPermissionFragment()
            4 -> AccessibilityServiceFragment()
            5 -> DefaultLauncherFragment()
            else -> IntroFragment()
        }
    }
}
