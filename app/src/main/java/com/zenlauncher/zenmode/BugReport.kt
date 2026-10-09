package com.zenlauncher.zenmode

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
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
 * Reports go to our community Telegram group, where the team reads every message and the
 * user can attach screenshots and follow the thread. Nothing is uploaded by the app: the
 * device details are copied to the clipboard, and the user decides whether to paste them.
 */
object BugReport {

    /**
     * The device/build block a report should carry. Deliberately small and legible: enough to
     * reproduce a bug (OEM ROM, Android version, app build) and nothing that identifies a person.
     */
    fun diagnostics(context: Context): String = buildString {
        appendLine("— $PRODUCT_NAME bug report —")
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})${if (BuildConfig.DEBUG) " debug" else ""}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Build: ${Build.DISPLAY}")
        appendLine("Locale: ${Locale.getDefault()}")
        appendLine("Pro: ${if (ProAccess.isPro(context)) "yes" else "no"}")
    }

    /** Copies [diagnostics] for the user to paste, then opens the Telegram group. */
    fun openInTelegram(context: Context) {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("$PRODUCT_NAME device details", diagnostics(context)))
        // Android 13+ shows its own "copied" confirmation; a second toast would just repeat it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Device details copied — paste them with your report.", Toast.LENGTH_LONG).show()
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TELEGRAM_URL)))
            ServiceLocator.analyticsManager.trackEvent("bug_report_sent", mapOf("channel" to "telegram"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app can open Telegram links.", Toast.LENGTH_SHORT).show()
        }
    }
}
