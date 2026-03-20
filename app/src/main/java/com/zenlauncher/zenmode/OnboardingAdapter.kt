package com.zenlauncher.zenmode

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 7

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IntroFragment()
            1 -> UsagePermissionFragment()
            2 -> OverlayPermissionFragment()
            3 -> NotificationPermissionFragment()
            4 -> GoogleSignInFragment()
            5 -> DefaultLauncherFragment()
            6 -> com.zenlauncher.zenmode.AccessibilityFragment()
            else -> IntroFragment()
        }
    }
}
