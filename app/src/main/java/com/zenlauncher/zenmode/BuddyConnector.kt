package com.zenlauncher.zenmode

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import kotlinx.coroutines.TimeoutCancellationException

/** Puts a buddy [code] on the clipboard, confirming with a toast when [confirm]. */
fun copyInviteCode(context: Context, code: String, confirm: Boolean = true) {
    context.getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("$PRODUCT_NAME Code", code))
    if (confirm) Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
}

/**
 * The "My Zen Circle" connect actions — share a link, trade codes, random connect —
 * shared by the home screen and onboarding. [onBuddyChanged] runs after a new buddy is
 * saved so the host can refresh whatever it shows.
 */
class BuddyConnector(
    private val activity: Activity,
    private val repository: UsageRepository,
    private val onBuddyChanged: () -> Unit = {}
) {

    fun copyUserCode(code: String, showToast: Boolean) {
        copyInviteCode(activity, code, confirm = showToast)
        ServiceLocator.analyticsTracker.trackBuddyCodeCopied("manual")
    }

    /** "Share a link": this user's invite link, via the system share sheet. */
    fun shareBuddyInvite(code: String) {
        ServiceLocator.analyticsTracker.trackBuddyShareStarted("link")
        // One tap for anyone who already has ZenMode installed (App Links opens straight
        // into the Connect screen via MainActivity.handleDeepLink); the same link also works
        // with no app installed - the zenmodeos.com/b/ page there points to the Play Store instead.
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Be my Zen Bro on $PRODUCT_NAME")
            putExtra(Intent.EXTRA_TEXT, inviteMessage(code))
        }
        activity.startActivity(Intent.createChooser(intent, "Share invite"))
    }

    /** "Share a link" for a real Zen Circle -- separate from [shareBuddyInvite]: a different
     * path (/c/ not /b/) and a different landing (join, not connect). */
    fun shareCircleInvite(circleId: String) {
        ServiceLocator.analyticsTracker.trackBuddyShareStarted("circle_link")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join my Zen Circle on $PRODUCT_NAME")
            putExtra(Intent.EXTRA_TEXT, circleInviteMessage(circleId))
        }
        activity.startActivity(Intent.createChooser(intent, "Share invite"))
    }

    suspend fun addBuddy(targetUid: String): BuddyAddResult {
        val currentUserId = ServiceLocator.authProvider.getCurrentUserId()

        if (targetUid == currentUserId) return BuddyAddResult.SelfAdd

        ServiceLocator.analyticsTracker.trackBuddyCodePasted("manual")

        if (!isConnected()) {
            return BuddyAddResult.Error("No internet connection. Please check your network and try again.")
        }

        val firestoreDataSource = ServiceLocator.firestoreDataSource
        return try {
            val user = firestoreDataSource.getUser(targetUid)
                ?: return BuddyAddResult.Error("User ID not found. Please check the ID and try again.")

            val myUid = currentUserId
                ?: return BuddyAddResult.Error("Not signed in.")

            if (firestoreDataSource.checkRelationshipExists(myUid, targetUid)) {
                return BuddyAddResult.AlreadyBuddies(user.displayName)
            }

            firestoreDataSource.sendBuddyInvite(myUid, targetUid)
            repository.clearCachedBuddy()
            onBuddyChanged()
            ServiceLocator.analyticsTracker.trackBuddyConnected("manual")

            BuddyAddResult.Success(user.displayName)
        } catch (e: TimeoutCancellationException) {
            BuddyAddResult.Error("Connection timed out. Please check your network and try again.")
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("offline", ignoreCase = true) == true ->
                    "Unable to connect. Please check your internet and try again."
                else -> "Failed: ${e.message}"
            }
            BuddyAddResult.Error(errorMessage)
        }
    }

    /** Returns the new buddy's display name on success, null otherwise (with a toast). */
    suspend fun randomConnect(): String? {
        val currentUserId = ServiceLocator.authProvider.getCurrentUserId()
        if (currentUserId == null) {
            RandomConnectOutcome.SignedOut.message?.let(::toast)
            return null
        }

        if (!isConnected()) {
            toast("No internet connection.")
            return null
        }

        return try {
            val outcome = RandomConnect.attempt(repository, currentUserId, ProAccess.isPro(activity)) {
                ServiceLocator.firestoreDataSource.findRandomBuddy(currentUserId)
            }
            if (outcome !is RandomConnectOutcome.Matched) {
                outcome.message?.let { toast(it, Toast.LENGTH_LONG) }
                return null
            }
            val buddy = ServiceLocator.firestoreDataSource.getUser(outcome.value)
            repository.clearCachedBuddy()
            repository.saveHasBuddy(true)
            onBuddyChanged()
            ServiceLocator.analyticsTracker.trackBuddyConnected("random")
            // No toast on success: the "You're Zen Bros now" screen says it.
            buddy?.displayName?.takeIf { it.isNotBlank() } ?: RandomConnect.UNNAMED_BRO
        } catch (_: TimeoutCancellationException) {
            toast("Connection timed out. Please try again.")
            null
        } catch (e: Exception) {
            toast("Something went wrong. Please try again.")
            null
        }
    }

    private fun isConnected(): Boolean {
        val connectivityManager = activity.getSystemService(ConnectivityManager::class.java)
        return connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun toast(message: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(activity, message, length).show()
    }

    companion object {
        /** The invite text: the user's zenmodeos.com/b/ invite link. Shared by the link and the circle card. */
        fun inviteMessage(code: String): String =
            "Be my Zen Bro on $PRODUCT_NAME! ${AppConstants.BUDDY_INVITE_BASE_URL}$code"

        /** Same idea for a real Zen Circle -- separate path/message, see [shareCircleInvite]. */
        fun circleInviteMessage(circleId: String): String =
            "Join my Zen Circle on $PRODUCT_NAME! ${AppConstants.CIRCLE_INVITE_BASE_URL}$circleId"
    }
}
