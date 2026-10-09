package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import java.util.Locale

/**
 * Settings → "Report a bug".
 *
 * SOURCE OF TRUTH for what a bug report contains and where it goes. The sheet
 * ([com.zenlauncher.zenmode.ui.components.BugReportSheet]) only explains it; everything about
 * the details and the destination lives here, so a report from any surface reads the same.
 *
 * Reports go to the helpdesk inbox through the user's own mail app: they write it and attach
 * screenshots there, and can read every line of it, the device details included, before it's
 * sent. Nothing is uploaded by the app. A phone with no mail app gets the community Telegram
 * group instead, where the team also reads every message.
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

    /**
     * Opens a mail to the helpdesk with the subject and device details filled in, room above
     * them for what happened. Falls back to the Telegram group when nothing can send mail.
     */
    fun send(context: Context) {
        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppConstants.SUPPORT_EMAIL}")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConstants.SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "$SUBJECT_PREFIX · ${Build.MANUFACTURER} ${Build.MODEL}")
            putExtra(Intent.EXTRA_TEXT, "\n\n\n${diagnostics(context)}")
        }
        try {
            context.startActivity(mail)
            ServiceLocator.analyticsManager.trackEvent("bug_report_sent", mapOf("channel" to "email"))
        } catch (_: ActivityNotFoundException) {
            openTelegram(context)
        }
    }

    /** No mail app on the phone: the Telegram group is where a person still reads it. */
    private fun openTelegram(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TELEGRAM_URL)))
            ServiceLocator.analyticsManager.trackEvent("bug_report_sent", mapOf("channel" to "telegram"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Email ${AppConstants.SUPPORT_EMAIL} to report it.", Toast.LENGTH_LONG).show()
        }
    }
}
