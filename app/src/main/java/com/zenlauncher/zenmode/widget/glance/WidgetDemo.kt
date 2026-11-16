package com.zenlauncher.zenmode.widget.glance

import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.widget.CircleSeat
import com.zenlauncher.zenmode.widget.CircleState
import com.zenlauncher.zenmode.widget.GoldState
import com.zenlauncher.zenmode.widget.QuietState
import com.zenlauncher.zenmode.week.WeekReader
import com.zenlauncher.zenmode.widget.WidgetData
import com.zenlauncher.zenmode.widget.faces.BloomCell
import java.time.LocalDate

/**
 * The week the widget picker shows (generated previews, Android 15 and later): a real-looking
 * Thursday, three good days in and one to go, nine weeks into a streak. Real content, as
 * Android's widget quality bar asks (WD-4), never the person's own data.
 */
object WidgetDemo {

    fun data(today: LocalDate = LocalDate.now()): WidgetData {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val thursday = monday.plusDays(3)
        val measured = buildMap {
            for (w in 1..9) for (d in 0 until 7) put(monday.minusWeeks(w.toLong()).plusDays(d.toLong()), if (d < 4) 72 else 52)
            put(monday, 78)
            put(monday.plusDays(1), 69)
            put(monday.plusDays(2), 52)
        }
        val week = WeekReader.read(measured, emptyMap(), thursday, 74, DayPart.AFTERNOON)
        return WidgetData(
            now = System.currentTimeMillis(),
            today = thursday,
            part = DayPart.AFTERNOON,
            usageAccess = true,
            week = week,
            todayScore = 74,
            yesterdayScore = 71,
            minutesToday = 112,
            promiseHours = 4,
            quiet = QuietState(available = true, running = null, ended = null),
            gold = GoldState("2,724", invested = true, open = false, kept = 3, outOfReach = false),
            circle = CircleState.Ranked(
                "Morning people",
                listOf(CircleSeat("Asha", 86, false), CircleSeat("You", 74, true), CircleSeat("Ravi", 68, false), CircleSeat("Meera", 61, false)),
                myRank = 2
            )
        )
    }

    /** Thursday afternoon's bloom: mornings mostly away, a few unplanned late evenings. */
    val bloom: List<BloomCell> = List(7 * 24) { i ->
        val d = i / 24
        val h = i % 24
        when {
            d > 3 || (d == 3 && h > 15) -> BloomCell.FUTURE
            d == 3 && h == 15 -> BloomCell.NOW
            h < 7 -> BloomCell.REST
            h in 9..12 && (d + h) % 3 != 0 -> BloomCell.AWAY
            h >= 21 && (d * 7 + h) % 4 == 0 -> BloomCell.UNPLANNED
            (d + h) % 3 == 0 -> BloomCell.ON_PURPOSE
            else -> BloomCell.LIGHT
        }
    }
}
