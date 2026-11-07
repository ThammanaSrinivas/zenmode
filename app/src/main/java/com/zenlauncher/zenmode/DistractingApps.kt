package com.zenlauncher.zenmode

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * The one rule for "this app is a feed": the known short-form/social packages, plus anything
 * the developer filed under social, video or games. Onboarding uses it to keep these off the
 * starting home apps; search uses it to keep them out of the suggestions shown before typing.
 * People can still pick or search for these apps by hand.
 */
object DistractingApps {

    private val PACKAGES = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.google.android.youtube",
        "com.netflix.mediaclient"
    )

    fun isDistracting(pm: PackageManager, pkg: String): Boolean {
        if (pkg in PACKAGES) return true
        val category = runCatching { pm.getApplicationInfo(pkg, 0).category }.getOrNull()
        return category == ApplicationInfo.CATEGORY_SOCIAL ||
            category == ApplicationInfo.CATEGORY_VIDEO ||
            category == ApplicationInfo.CATEGORY_GAME
    }
}
