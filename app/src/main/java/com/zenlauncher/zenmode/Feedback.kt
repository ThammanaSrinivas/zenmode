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
 * Settings → "Report a bug" and "Suggest a feature".
 *
 * SOURCE OF TRUTH for what a report or a request contains and where it goes. The sheet
 * ([com.zenlauncher.zenmode.ui.components.FeedbackSheet]) only explains it; everything about
 * the details and the destination lives here, so both read the same from any surface.
 *
 * Both go to the helpdesk inbox through the person's own mail app: they write it and attach
 * screenshots there, and can read every line of it, the device details included, before it's
 * sent. Nothing is uploaded by the app. A phone with no mail app gets the community Telegram
 * group instead, where the team also reads every message.
 */
object Feedback {

    /** What is being sent: the mail subject and the analytics event follow from it. */
    enum class Kind(val subject: String, val sentEvent: String) {
        BUG("$PRODUCT_NAME bug report", "bug_report_sent"),
        IDEA("$PRODUCT_NAME feature request", "feature_request_sent")
    }

    /**
     * The device/build block appended to every mail. Deliberately small and legible: enough to
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
     * them for the words. Falls back to the Telegram group when nothing can send mail.
     */
    fun send(context: Context, kind: Kind) {
        val subject = "${kind.subject} · ${Build.MANUFACTURER} ${Build.MODEL}"
        val body = "\n\n\n${diagnostics(context)}"
        val mail = Intent(Intent.ACTION_SENDTO, mailto(subject, body)).apply {
            // Some mail apps read these instead of the link; Gmail reads only the link.
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConstants.SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(mail)
            ServiceLocator.analyticsManager.trackEvent(kind.sentEvent, mapOf("channel" to "email"))
        } catch (_: ActivityNotFoundException) {
            openTelegram(context, kind)
        }
    }

    /**
     * The address with the subject and the device details in the link itself. Gmail ignores a
     * SENDTO intent's extras, so without this the mail arrived empty but for the address.
     */
    fun mailto(subject: String, body: String): Uri =
        Uri.parse("mailto:${AppConstants.SUPPORT_EMAIL}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")

    /** No mail app on the phone: the Telegram group is where a person still reads it. */
    private fun openTelegram(context: Context, kind: Kind) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TELEGRAM_URL)))
            ServiceLocator.analyticsManager.trackEvent(kind.sentEvent, mapOf("channel" to "telegram"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Email ${AppConstants.SUPPORT_EMAIL}, we read every one.", Toast.LENGTH_LONG).show()
        }
    }
}
