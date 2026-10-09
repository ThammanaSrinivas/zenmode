package com.zenlauncher.zenmode.share

import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.GoldOrder
import com.zenlauncher.zenmode.ZenGoldPromiseState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What a Zen Gold card shows. Until there's gold in the account (the balance is still a
 * placeholder ₹0 — see AppConstants), the card is about the week's promise, which is real:
 * coins filling towards gold pay, then gold pay unlocked. Once there's a balance, the picture
 * grows with it — a first coin, stacks, bars, a vault.
 */
enum class GoldTier {
    EARNING, UNLOCKED, FIRST, STACK, BARS, VAULT;

    companion object {
        fun of(investedRupees: Long, goldPayOpen: Boolean): GoldTier = when {
            investedRupees >= 1_00_000 -> VAULT
            investedRupees >= 10_000 -> BARS
            investedRupees >= 1_000 -> STACK
            investedRupees > 0 -> FIRST
            goldPayOpen -> UNLOCKED
            else -> EARNING
        }
    }
}

/**
 * Everything a Zen Gold card says, from the Home pill's balance and this week's real promise
 * state ([com.zenlauncher.zenmode.ZenGoldPromise.weekly]).
 */
data class GoldShare(
    val investedRupees: Long,
    val week: ZenGoldPromiseState,
    val today: LocalDate
) {
    val tier: GoldTier = GoldTier.of(investedRupees, week.goalMet)
    val kept: Int = week.unitsKept
    val needed: Int = AppConstants.PROMISE_DAYS_TO_UNLOCK

    val amount: String = rupeesLabel(investedRupees)

    val title: String = when (tier) {
        GoldTier.EARNING -> "Earning my gold."
        GoldTier.UNLOCKED -> "Gold pay, unlocked."
        GoldTier.FIRST -> "My first gold."
        GoldTier.STACK -> "Stacking gold."
        GoldTier.BARS -> "Solid gold."
        GoldTier.VAULT -> "A vault of calm."
    }

    val caption: String = when (tier) {
        GoldTier.EARNING -> when {
            week.goalOutOfReach -> "Not this week. Monday brings a fresh one, and a fresh shot at gold."
            kept == 0 -> "A new week, a promise of ${week.promiseHours} hours a day. Keep $needed days and it turns into gold."
            else -> "$kept of $needed promise days kept this week. ${needed - kept} more and my calm turns into gold."
        }
        GoldTier.UNLOCKED -> "Kept my promise $kept of 7 days this week. Calm, turned into gold."
        GoldTier.FIRST -> "$amount of screen time I didn't scroll, now sitting in gold."
        GoldTier.STACK -> "$amount in gold, one kept promise at a time."
        GoldTier.BARS -> "$amount in gold, built from quiet hours instead of loud ones."
        GoldTier.VAULT -> "$amount in gold. Every rupee of it a promise I kept."
    }

    val stamp: String = "ZEN GOLD"

    val stats: List<PosterStat> = when (tier) {
        GoldTier.EARNING, GoldTier.UNLOCKED -> listOf(
            PosterStat("DAYS KEPT", "$kept/7"),
            PosterStat("PROMISE", "${week.promiseHours}H/DAY"),
            PosterStat("GOLD PAY", if (week.goalMet) "OPEN" else "AT $needed")
        )
        else -> listOf(
            PosterStat("IN GOLD", amount),
            PosterStat("PROMISE", "${week.promiseHours}H/DAY"),
            PosterStat("THIS WEEK", "$kept/7 KEPT")
        )
    }

    val footer: String = today.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)).uppercase(Locale.US)

    /** The line under the card in the sheet. */
    val nextStep: String = when (tier) {
        GoldTier.EARNING -> if (week.goalOutOfReach) {
            "A fresh week starts Monday."
        } else {
            "${plural(needed - kept, "more kept day")} and gold pay opens."
        }
        GoldTier.UNLOCKED -> "Gold pay stays open until Sunday midnight."
        // GoldLedger: the hand-off is the purchase signal, so say what the total counts.
        else -> "Counts the orders you opened in Kite from Zen Gold."
    }

    val shareText: String = when (tier) {
        GoldTier.EARNING -> "Turning my screen time into gold with $PRODUCT_NAME — $kept of $needed promise days kept " +
            "this week. ${AppConstants.PLAY_STORE_URL}"
        GoldTier.UNLOCKED -> "Kept my screen-time promise $kept of 7 days this week, and unlocked gold pay on " +
            "$PRODUCT_NAME. ${AppConstants.PLAY_STORE_URL}"
        else -> "$amount of screen time turned into gold with $PRODUCT_NAME. Start turning yours: " +
            AppConstants.PLAY_STORE_URL
    }

    val description: String = "$title $caption"

    /** What analytics calls this card. */
    val analyticsKey: String = "gold_${tier.name.lowercase(Locale.US)}"

    companion object {
        /** "₹2,350", "₹1,23,456": whole rupees in the same Indian grouping as everywhere else. */
        fun rupeesLabel(rupees: Long): String = GoldOrder.formatInr(rupees * 100).removeSuffix(".00")

        /** The Home pill's "2,350" → 2350; anything unreadable is ₹0. */
        fun rupees(formatted: String): Long = formatted.filter { it.isDigit() }.toLongOrNull() ?: 0L
    }
}
