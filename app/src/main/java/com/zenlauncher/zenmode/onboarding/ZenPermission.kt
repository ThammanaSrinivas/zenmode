package com.zenlauncher.zenmode.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.zenlauncher.zenmode.ZenAccessibilityService
import com.zenlauncher.zenmode.coreapi.UsageAccess

/**
 * The device grants ZenMode asks for, with the one place that checks each. [analyticsKey]
 * matches the keys the v2 permission screens tracked, so funnels stay comparable.
 */
enum class ZenPermission(
    val analyticsKey: String,
    val title: String,
    val reason: String,
    val required: Boolean
) {
    USAGE_ACCESS(
        analyticsKey = "usage",
        title = "Usage access",
        reason = "Powers your Zen Score and screen-time promise.",
        required = true
    ),
    ACCESSIBILITY(
        analyticsKey = "acc",
        title = "Reels & Shorts blocker",
        reason = "Hides endless feeds inside apps. Nothing leaves your phone.",
        required = false
    ),
    NOTIFICATIONS(
        analyticsKey = "notifications",
        title = "Notifications",
        reason = "Hear it when your Zen Bro cheers you on.",
        required = false
    );

    fun isGranted(context: Context): Boolean = when (this) {
        USAGE_ACCESS -> UsageAccess.isGranted(context)
        ACCESSIBILITY -> ZenAccessibilityService.isEnabledInSettings(context)
        NOTIFICATIONS -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Settings screen for grants that live there; null for [NOTIFICATIONS], a runtime prompt. */
    fun settingsIntent(context: Context): Intent? = when (this) {
        USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        NOTIFICATIONS -> null
    }

    companion object {
        /** What this device can be asked for — the notification prompt only exists on 13+. */
        fun applicable(): List<ZenPermission> =
            entries.filter { it != NOTIFICATIONS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }

        fun grantedSet(context: Context): Set<ZenPermission> =
            applicable().filter { it.isGranted(context) }.toSet()

        fun hasAllRequired(granted: Set<ZenPermission>): Boolean =
            entries.filter { it.required }.all { it in granted }
    }
}
