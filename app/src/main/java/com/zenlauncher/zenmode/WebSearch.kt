package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Where the home search bar hands off to, once it has run out of apps and files to answer with.
 *
 * SOURCE OF TRUTH for both hand-offs, so the search bar and anything else that needs them
 * behave the same.
 */
object WebSearch {

    private const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
    /** Google Lens lives inside the Google app; this is its exported entry point. */
    private const val LENS_ACTIVITY = "com.google.android.apps.search.lens.LensExportedActivity"
    private const val LENS_PLAY_URL =
        "https://play.google.com/store/apps/details?id=$GOOGLE_PACKAGE"

    /**
     * Runs [query] and lands on the results.
     *
     * Deliberately *not* `ACTION_WEB_SEARCH`: recent Google app versions treat that as "open
     * the search box with this text in it", so the user had to tap search a second time to get
     * anywhere. A results URL is unambiguous — one tap, results — and still opens inside the
     * Google app when it's installed and holds the link, because it claims google.com/search.
     */
    fun run(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val results = Uri.parse("https://www.google.com/search?q=${Uri.encode(trimmed)}")
        val view = Intent(Intent.ACTION_VIEW, results).addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            context.startActivity(view)
        } catch (_: ActivityNotFoundException) {
            // No browser and no Google app: nothing can open a web result.
            Toast.makeText(context, "No browser to search with.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens Google Lens, for scanning a QR code or pointing the camera at something.
     *
     * Lens has no public intent contract, so this goes through its exported activity and falls
     * back, in order, to the Google app itself and then its Play listing — rather than dying on
     * a phone that ships a Google app too old to have the activity.
     */
    fun openLens(context: Context) {
        val lens = Intent(Intent.ACTION_VIEW)
            .setClassName(GOOGLE_PACKAGE, LENS_ACTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (lens.resolveActivity(context.packageManager) != null) {
            runCatching { context.startActivity(lens) }.onSuccess { return }
        }
        val google = context.packageManager.getLaunchIntentForPackage(GOOGLE_PACKAGE)
        if (google != null) {
            context.startActivity(google)
            return
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LENS_PLAY_URL)))
        }.onFailure {
            Toast.makeText(context, "Install the Google app to scan.", Toast.LENGTH_SHORT).show()
        }
    }

    /** True when the phone has something that can run Lens — the bar hides the button if not. */
    fun isLensAvailable(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(GOOGLE_PACKAGE, 0) }
            .getOrNull() != null
}
