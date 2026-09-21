package com.zenlauncher.zenmode.recap

import com.zenlauncher.zenmode.coreapi.ZenScore
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DataExportTest {

    private val monday = LocalDate.of(2026, 9, 7)

    private fun day(offset: Long, minutes: Long, pickups: Int? = 30, apps: List<AppMinutes> = emptyList()) =
        DayRecord(monday.plusDays(offset), minutes, promiseHours = 3, appMinutes = apps, lateNightMinutes = 12, pickups = pickups)

    @Test
    fun `header is the first line`() {
        assertEquals(DataExport.HEADER.joinToString(","), DataExport.csv(emptyList()).lines().first())
    }

    @Test
    fun `rows run oldest day first`() {
        val lines = DataExport.csv(listOf(day(2, 100), day(0, 100), day(1, 100))).trim().lines().drop(1)
        assertEquals(listOf("2026-09-07", "2026-09-08", "2026-09-09"), lines.map { it.substringBefore(',') })
    }

    @Test
    fun `a row carries the day's numbers, promise and score`() {
        val row = DataExport.csv(listOf(day(0, 150, apps = listOf(AppMinutes("pkg", "Maps", 40))))).trim().lines()[1]
        val score = ZenScore.format(ZenScore.compute(150 * 60_000L, 30, 3))
        assertEquals("2026-09-07,150,3,yes,$score,30,12,Maps,40", row)
    }

    @Test
    fun `missing pickups leave the score and pickups blank rather than guessing`() {
        val row = DataExport.csv(listOf(day(0, 500, pickups = null))).trim().lines()[1]
        assertEquals("2026-09-07,500,3,no,,,12,,", row)
    }

    @Test
    fun `app names with commas or quotes are escaped`() {
        assertEquals("\"Hello, World\"", DataExport.escape("Hello, World"))
        assertEquals("\"Say \"\"hi\"\"\"", DataExport.escape("Say \"hi\""))
        assertEquals("Plain", DataExport.escape("Plain"))
    }
}
