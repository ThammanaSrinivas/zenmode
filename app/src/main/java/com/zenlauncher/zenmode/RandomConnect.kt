package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

/** How one Random Connect tap ended. */
sealed interface RandomConnectOutcome<out T> {
    data class Matched<T>(val value: T) : RandomConnectOutcome<T>
    /** The last try found no one; retrying is blocked for [secondsLeft] more seconds. */
    data class CoolingDown(val secondsLeft: Long) : RandomConnectOutcome<Nothing>
    data class OutOfQuota(val weeklyLimit: Int, val isPro: Boolean) : RandomConnectOutcome<Nothing>
    data object NoneAvailable : RandomConnectOutcome<Nothing>
    data object SignedOut : RandomConnectOutcome<Nothing>
}

/** What to tell the user when a tap didn't pair them with anyone; null on a match. */
val RandomConnectOutcome<*>.message: String?
    get() = when (this) {
        is RandomConnectOutcome.Matched -> null
        is RandomConnectOutcome.CoolingDown -> "No one was available last time. Try again in ${secondsLeft}s."
        is RandomConnectOutcome.OutOfQuota -> if (isPro) {
            "You've used all $weeklyLimit random connects this week. More open up next week."
        } else {
            "You've used all $weeklyLimit random connects this week. Upgrade to Pro for up to ${Entitlement.RANDOM_CONNECT_PRO_WEEKLY_LIMIT}/week."
        }
        RandomConnectOutcome.NoneAvailable ->
            "No one available right now. Try again in ${AppConstants.RANDOM_CONNECT_COOLDOWN_MS / 1000} seconds!"
        RandomConnectOutcome.SignedOut -> "Not signed in."
    }

/**
 * Random Connect's rules, the same for a Zen Bro and a Zen Circle: a cooldown after a try that
 * found no one, and one weekly allowance shared by both kinds (Pro gets more).
 */
object RandomConnect {

    /** The feature's name wherever it's offered or compared. */
    const val NAME = "Random connect"

    /** The matched person's name when they never set one. */
    const val UNNAMED_BRO = "your Zen Bro"

    /**
     * Runs [match] if the cooldown and this week's allowance allow it. [match] does the pairing
     * and returns null when no one was free; only a real match uses up the allowance.
     */
    suspend fun <T : Any> attempt(
        repository: UsageRepository,
        myUid: String,
        isPro: Boolean,
        match: suspend () -> T?
    ): RandomConnectOutcome<T> {
        val sinceLastTry = System.currentTimeMillis() - repository.getLastRandomConnectAttemptTime()
        val remaining = AppConstants.RANDOM_CONNECT_COOLDOWN_MS - sinceLastTry
        if (remaining > 0) return RandomConnectOutcome.CoolingDown((remaining / 1000).coerceAtLeast(1))

        val firestore = ServiceLocator.firestoreDataSource
        val weeklyLimit = if (isPro) Entitlement.RANDOM_CONNECT_PRO_WEEKLY_LIMIT else Entitlement.RANDOM_CONNECT_FREE_WEEKLY_LIMIT
        if (!firestore.hasRandomConnectQuota(myUid, weeklyLimit)) {
            return RandomConnectOutcome.OutOfQuota(weeklyLimit, isPro)
        }

        val matched = match()
        if (matched == null) {
            repository.saveLastRandomConnectAttemptTime(System.currentTimeMillis())
            return RandomConnectOutcome.NoneAvailable
        }
        firestore.recordRandomConnectUsed(myUid)
        return RandomConnectOutcome.Matched(matched)
    }
}
