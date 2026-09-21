package com.zenlauncher.zenmode.coreapi

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * Maps a package to one of the four [AppCategory] buckets, and to its display label.
 *
 * Primary signal is [ApplicationInfo.category] (API 26+, Play-Store-declared, free/local —
 * no new permission beyond the QUERY_ALL_PACKAGES this app already holds). Coverage is
 * incomplete by design: it's optional metadata and plenty of apps leave it undeclared, which
 * falls into [AppCategory.OTHER] below.
 *
 * Android's own taxonomy has no messaging category — CATEGORY_SOCIAL covers everything from
 * a doomscroll feed (Instagram, X) to a 1:1 chat app (WhatsApp, Telegram), and those read very
 * differently for session quality. [COMMUNICATION_OVERRIDES] pulls the well-known messaging
 * apps out into [AppCategory.COMMUNICATION]; anything else tagged SOCIAL defaults to
 * [AppCategory.ENTERTAINMENT], which matches how those apps actually get used.
 */
object AppCategoryClassifier {

    private val COMMUNICATION_OVERRIDES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.google.android.apps.messaging",
        "com.facebook.orca",
        "com.discord",
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.skype.raider",
        "com.slack"
    )

    fun classify(packageManager: PackageManager, packageName: String): AppCategory {
        if (packageName in COMMUNICATION_OVERRIDES) return AppCategory.COMMUNICATION
        return when (rawCategory(packageManager, packageName)) {
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.FOCUS
            ApplicationInfo.CATEGORY_GAME,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_IMAGE,
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.ENTERTAINMENT
            else -> AppCategory.OTHER
        }
    }

    /** Display name for a package, falling back to a title-cased tail of the package id. */
    fun labelOf(packageManager: PackageManager, packageName: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun rawCategory(packageManager: PackageManager, packageName: String): Int = try {
        packageManager.getApplicationInfo(packageName, 0).category
    } catch (_: PackageManager.NameNotFoundException) {
        ApplicationInfo.CATEGORY_UNDEFINED
    }
}
