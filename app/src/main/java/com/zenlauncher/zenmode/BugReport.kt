package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import java.util.Locale

/**
 * Settings → "Report a bug".
 *
 * SOURCE OF TRUTH for what a bug report contains and where it goes. The composer
 * ([com.zenlauncher.zenmode.ui.components.BugReportSheet]) only collects what the user types
 * and an optional screenshot; everything about the body, the address and the fallback lives here,
 * so a report sent from any surface reads the same on our side.
 *
 * Nothing is uploaded: the report leaves through the user's own mail app, so they can read and
 * edit every line of it — including the diagnostics — before it is sent. That matches the privacy
 * promise on the rest of the app, where the data stays on the phone unless the user hands it over.
 */
object BugReport {

    /** What the mail subject carries, so reports thread sensibly in the helpdesk inbox. */
    private const val SUBJECT_PREFIX = "$PRODUCT_NAME bug report"

    /**
     * The device/build block appended to every report. Deliberately small and legible: enough to
     * reproduce a bug (OEM ROM, Android version, app build) and nothing that identifies a person.
     */
    fun diagnostics(context: Context): String = buildString {
        appendLine("— sent from $PRODUCT_NAME, please keep the lines below —")
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})${if (BuildConfig.DEBUG) " debug" else ""}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Build: ${Build.DISPLAY}")
        appendLine("Locale: ${Locale.getDefault()}")
        appendLine("Pro: ${if (ProAccess.isPro(context)) "yes" else "no"}")
    }

    /** The full mail body: what the user wrote, then the diagnostics block. */
    fun body(context: Context, description: String): String =
        "${description.trim()}\n\n\n${diagnostics(context)}"

    /**
     * Hands the report to the user's mail app, with [screenshot] attached when they picked one.
     * Returns false when the phone has no app that can send it — the caller tells the user and
     * offers the Telegram group instead, the same fallback the early-access request uses.
     */
    fun send(context: Context, description: String, screenshot: Uri? = null): Boolean {
        val intent = Intent(if (screenshot != null) Intent.ACTION_SEND else Intent.ACTION_SENDTO).apply {
            if (screenshot != null) {
                // ACTION_SEND needs a type and can't use a mailto: uri, so the address goes in
                // EXTRA_EMAIL and the chooser is limited to apps that take an image.
                type = "image/*"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConstants.SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_STREAM, screenshot)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                // mailto: keeps the chooser to real mail apps rather than every share target.
                data = Uri.parse("mailto:${AppConstants.SUPPORT_EMAIL}")
            }
            putExtra(Intent.EXTRA_SUBJECT, "$SUBJECT_PREFIX · ${Build.MANUFACTURER} ${Build.MODEL}")
            putExtra(Intent.EXTRA_TEXT, body(context, description))
        }
        return try {
            context.startActivity(Intent.createChooser(intent, "Send bug report"))
            ServiceLocator.analyticsManager.trackEvent(
                "bug_report_sent",
                mapOf("has_screenshot" to (screenshot != null))
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** No mail app on the phone: the Telegram group is where a person still reads it. */
    fun openFallback(context: Context) {
        ServiceLocator.analyticsManager.trackEvent("bug_report_fallback", emptyMap())
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TELEGRAM_URL)))
    }
}
