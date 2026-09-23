package com.zenlauncher.zenmode

/**
 * The Invest Gold quantity picker's arithmetic (Figma node 2026:1250). Money is held
 * in paise so a unit price × quantity never picks up floating-point drift, and every
 * rupee string on the screen comes from [formatInr] so they all agree.
 */
object GoldOrder {

    fun clampUnits(units: Int): Int =
        units.coerceIn(AppConstants.INVEST_GOLD_MIN_UNITS, AppConstants.INVEST_GOLD_MAX_UNITS_PER_WEEK)

    fun totalPaise(units: Int, unitPricePaise: Long): Long = clampUnits(units) * unitPricePaise

    fun weeklyCapPaise(unitPricePaise: Long): Long =
        AppConstants.INVEST_GOLD_MAX_UNITS_PER_WEEK * unitPricePaise

    /**
     * The Gold Invested change percent (Home + Zen Gold). There's no live price-tracking
     * backend yet (see AppConstants' gold placeholders), so this can't compute a real
     * gain/loss — but it can stay honest: ₹0 invested can't have gained anything, so the
     * percent is 0 rather than a hardcoded figure that contradicts a zero balance. Once a
     * real portfolio backend exists, replace this with the actual gain/loss over cost basis.
     */
    fun changePercentFor(investedRupees: String): Int =
        if (investedRupees.toLongOrNull() == 0L) 0 else AppConstants.PLACEHOLDER_GOLD_CHANGE_PERCENT

    /** "₹1,23,456.70" — Indian digit grouping (last three, then pairs), always two decimals. */
    fun formatInr(paise: Long): String {
        val sign = if (paise < 0) "-" else ""
        val abs = Math.abs(paise)
        val rupees = (abs / 100).toString()
        val decimals = (abs % 100).toString().padStart(2, '0')
        val grouped = if (rupees.length <= 3) {
            rupees
        } else {
            val head = rupees.dropLast(3)
            head.reversed().chunked(2).joinToString(",").reversed() + "," + rupees.takeLast(3)
        }
        return "$sign₹$grouped.$decimals"
    }
}
