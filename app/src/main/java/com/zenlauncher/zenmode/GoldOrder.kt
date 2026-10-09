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
