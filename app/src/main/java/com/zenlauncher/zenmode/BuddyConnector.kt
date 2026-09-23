package com.zenlauncher.zenmode

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import kotlinx.coroutines.TimeoutCancellationException

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
        val clipboard = activity.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("ZenMode Code", code))
        ServiceLocator.analyticsTracker.trackBuddyCodeCopied("manual")
        if (showToast) {
            Toast.makeText(activity, "Code copied!", Toast.LENGTH_SHORT).show()
        }
    }

    /** "Share a link": this user's invite link, via the system share sheet. */
    fun shareBuddyInvite(code: String) {
        ServiceLocator.analyticsTracker.trackBuddyShareStarted("link")
        // One tap for anyone who already has ZenMode installed (App Links opens straight
        // into the Connect screen via MainActivity.handleDeepLink); the same link also works
        // with no app installed - the zenmodeos.com/b/ page there points to the Play Store instead.
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Be my Zen Bro on ZenMode")
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
            putExtra(Intent.EXTRA_SUBJECT, "Join my Zen Circle on ZenMode")
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
            toast("Not signed in.")
            return null
        }

        if (!isConnected()) {
            toast("No internet connection.")
            return null
        }

        // Cooldown: only allow retrying after cooldown if last attempt found no buddy
        val lastTried = repository.getLastRandomConnectAttemptTime()
        val remaining = AppConstants.RANDOM_CONNECT_COOLDOWN_MS - (System.currentTimeMillis() - lastTried)
        if (remaining > 0) {
            val secs = (remaining / 1000).coerceAtLeast(1)
            toast("No buddies were available last time. Try again in ${secs}s.", Toast.LENGTH_LONG)
            return null
        }

        val isPro = ProAccess.isPro(activity)
        val weeklyLimit = if (isPro) Entitlement.RANDOM_CONNECT_PRO_WEEKLY_LIMIT else Entitlement.RANDOM_CONNECT_FREE_WEEKLY_LIMIT
        if (!ServiceLocator.firestoreDataSource.hasRandomConnectQuota(currentUserId, weeklyLimit)) {
            if (isPro) {
                toast("You've used all $weeklyLimit random connects this week. More open up next week.", Toast.LENGTH_LONG)
            } else {
                toast("You've used all $weeklyLimit random connects this week. Upgrade to Pro for up to ${Entitlement.RANDOM_CONNECT_PRO_WEEKLY_LIMIT}/week.", Toast.LENGTH_LONG)
            }
            return null
        }

        return try {
            val buddyUid = ServiceLocator.firestoreDataSource.findRandomBuddy(currentUserId)
            if (buddyUid == null) {
                repository.saveLastRandomConnectAttemptTime(System.currentTimeMillis())
                toast("No buddies available right now. Try again in 30 seconds!", Toast.LENGTH_LONG)
                null
            } else {
                ServiceLocator.firestoreDataSource.recordRandomConnectUsed(currentUserId)
                val buddy = ServiceLocator.firestoreDataSource.getUser(buddyUid)
                repository.clearCachedBuddy()
                repository.saveHasBuddy(true)
                onBuddyChanged()
                ServiceLocator.analyticsTracker.trackBuddyConnected("random")
                // No toast on success: the "You're Zen Bros now" screen says it.
                buddy?.displayName?.takeIf { it.isNotBlank() } ?: "your Zen Bro"
            }
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
            "Be my Zen Bro on ZenMode! ${AppConstants.BUDDY_INVITE_BASE_URL}$code"

        /** Same idea for a real Zen Circle -- separate path/message, see [shareCircleInvite]. */
        fun circleInviteMessage(circleId: String): String =
            "Join my Zen Circle on ZenMode! ${AppConstants.CIRCLE_INVITE_BASE_URL}$circleId"
    }
}
