package com.zenlauncher.zenmode

import android.content.Context
import com.zenlauncher.zenmode.coreapi.zenPrefs
import com.zenlauncher.zenmode.recap.weekStartOf
import java.time.LocalDate

/**
 * Zen Gold's balance: what the user has put into gold through ZenMode.
 *
 * ZenMode never sees the broker account, so there's no payment acknowledgement to wait for.
 * Until there is one, the hand-off counts as the purchase: tapping "Open Kite" on the order
 * review - reachable only in a week whose promise was kept - records that order. One order per
 * week: a second hand-off in the same week replaces the first instead of adding to it, so a
 * retry (Kite didn't open, the user came back and tapped again) can't count twice. The review
 * screen says this before the tap. On-device only. Decision: docs/adr/0002-gold-balance-counts-the-kite-hand-off.md.
 */
object GoldLedger {

    // "2026-10-05=37146;2026-10-12=12382": the Monday each order's week starts, and its paise.
    private const val KEY_ORDERS = "gold_ledger_orders"

    /** Records an order of [paise] for the week containing [today], replacing that week's earlier one. */
    fun record(context: Context, today: LocalDate, paise: Long) {
        val orders = withOrder(orders(context), weekStartOf(today), paise)
        context.zenPrefs().edit().putString(KEY_ORDERS, format(orders)).apply()
    }

    /** Every recorded order, by the Monday of its week. */
    fun orders(context: Context): Map<LocalDate, Long> = parse(context.zenPrefs().getString(KEY_ORDERS, null))

    /** The balance as Home and Zen Gold print it after a ₹: whole rupees, Indian grouping ("2,350"). */
    fun balanceLabel(context: Context): String = rupeesLabel(orders(context).values.sum())

    internal fun withOrder(orders: Map<LocalDate, Long>, weekStart: LocalDate, paise: Long): Map<LocalDate, Long> =
        (orders + (weekStart to paise)).toSortedMap()

    internal fun rupeesLabel(paise: Long): String =
        GoldOrder.formatInr((paise + 50) / 100 * 100).removePrefix("₹").removeSuffix(".00")

    internal fun format(orders: Map<LocalDate, Long>): String =
        orders.entries.joinToString(";") { (week, paise) -> "$week=$paise" }

    internal fun parse(raw: String?): Map<LocalDate, Long> =
        raw.orEmpty().split(";").mapNotNull { entry ->
            val (week, paise) = entry.split("=").takeIf { it.size == 2 } ?: return@mapNotNull null
            runCatching { LocalDate.parse(week) to paise.toLong() }.getOrNull()
        }.toMap().toSortedMap()
}
