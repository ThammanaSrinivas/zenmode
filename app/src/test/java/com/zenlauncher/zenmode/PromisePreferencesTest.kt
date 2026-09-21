package com.zenlauncher.zenmode

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class PromisePreferencesTest {

    // Same calendar week throughout, so a stub for "this week" applies to either day.
    private val weekStart = LocalDate.of(2026, 3, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    private val monday = weekStart
    private val sunday = weekStart.plusDays(6)

    private fun createMocks(): Triple<Context, SharedPreferences, SharedPreferences.Editor> {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)

        return Triple(context, prefs, editor)
    }

    /** No prior edit recorded this week. */
    private fun stubNoEditsYet(prefs: SharedPreferences) {
        whenever(prefs.getString(eq("promise_edit_week_start"), isNull())).thenReturn(null)
    }

    private fun stubEditsUsed(prefs: SharedPreferences, weekStart: LocalDate, count: Int) {
        whenever(prefs.getString(eq("promise_edit_week_start"), isNull())).thenReturn(weekStart.toString())
        whenever(prefs.getInt(eq("promise_edit_count"), eq(0))).thenReturn(count)
    }

    @Test
    fun `free user can edit on Sunday with no prior edits this week`() {
        val (context, prefs, _) = createMocks()
        stubNoEditsYet(prefs)

        assertEquals(PromiseEditLock.NONE, PromisePreferences.editLock(context, isPro = false, today = sunday))
    }

    @Test
    fun `free user is locked out on a non-Sunday`() {
        val (context, prefs, _) = createMocks()
        stubNoEditsYet(prefs)

        assertEquals(PromiseEditLock.SUNDAY_ONLY, PromisePreferences.editLock(context, isPro = false, today = monday))
    }

    @Test
    fun `free user is locked out after using their one edit this week`() {
        val (context, prefs, _) = createMocks()
        stubEditsUsed(prefs, weekStart, count = 1)

        assertEquals(
            PromiseEditLock.WEEKLY_LIMIT_REACHED,
            PromisePreferences.editLock(context, isPro = false, today = sunday)
        )
    }

    @Test
    fun `pro user can edit on any day`() {
        val (context, prefs, _) = createMocks()
        stubNoEditsYet(prefs)

        assertEquals(PromiseEditLock.NONE, PromisePreferences.editLock(context, isPro = true, today = monday))
    }

    @Test
    fun `pro user gets a second edit that free would be locked out of`() {
        val (context, prefs, _) = createMocks()
        stubEditsUsed(prefs, weekStart, count = 1)

        assertEquals(PromiseEditLock.NONE, PromisePreferences.editLock(context, isPro = true, today = monday))
    }

    @Test
    fun `pro user is locked out after using both edits this week`() {
        val (context, prefs, _) = createMocks()
        stubEditsUsed(prefs, weekStart, count = 2)

        assertEquals(
            PromiseEditLock.WEEKLY_LIMIT_REACHED,
            PromisePreferences.editLock(context, isPro = true, today = monday)
        )
    }

    @Test
    fun `a prior edit from last week doesn't count against this week's quota`() {
        val (context, prefs, _) = createMocks()
        stubEditsUsed(prefs, weekStart.minusWeeks(1), count = 1)

        assertEquals(PromiseEditLock.NONE, PromisePreferences.editLock(context, isPro = false, today = sunday))
    }

    @Test
    fun `recordPromiseEdit starts the count at 1 for a fresh week`() {
        val (context, prefs, editor) = createMocks()
        stubNoEditsYet(prefs)

        PromisePreferences.recordPromiseEdit(context, today = sunday)

        verify(editor).putString("promise_edit_week_start", weekStart.toString())
        verify(editor).putInt("promise_edit_count", 1)
    }

    @Test
    fun `recordPromiseEdit increments an existing count in the same week`() {
        val (context, prefs, editor) = createMocks()
        stubEditsUsed(prefs, weekStart, count = 1)

        PromisePreferences.recordPromiseEdit(context, today = monday)

        verify(editor).putInt("promise_edit_count", 2)
    }

    @Test
    fun `defaults to the placeholder promise`() {
        val (context, prefs, _) = createMocks()
        val default = AppConstants.PLACEHOLDER_PROMISE_HOURS
        whenever(prefs.getInt("promise_daily_hours", default)).thenReturn(default)

        assertEquals(default, PromisePreferences.getDailyHours(context))
    }

    @Test
    fun `clamps an out-of-range stored value`() {
        val (context, prefs, _) = createMocks()
        whenever(prefs.getInt("promise_daily_hours", AppConstants.PLACEHOLDER_PROMISE_HOURS)).thenReturn(99)

        assertEquals(AppConstants.PROMISE_MAX_DAILY_HOURS, PromisePreferences.getDailyHours(context))
    }

    @Test
    fun `setDailyHours persists the clamped value`() {
        val (context, _, editor) = createMocks()

        PromisePreferences.setDailyHours(context, 0)

        verify(editor).putInt("promise_daily_hours", AppConstants.PROMISE_MIN_DAILY_HOURS)
        verify(editor).apply()
    }
}
