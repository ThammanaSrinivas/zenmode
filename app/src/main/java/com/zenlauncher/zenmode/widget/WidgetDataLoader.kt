package com.zenlauncher.zenmode.widget

import android.content.Context
import com.zenlauncher.zenmode.GoldLedger
import com.zenlauncher.zenmode.ZenGoldPromise
import com.zenlauncher.zenmode.ZenScoreStore
import com.zenlauncher.zenmode.coreapi.PromisePreferences
import com.zenlauncher.zenmode.coreapi.UsageAccess
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.coreapi.week.DayPart
import com.zenlauncher.zenmode.coreapi.zenPrefs
import com.zenlauncher.zenmode.quiet.QuietSession
import com.zenlauncher.zenmode.recap.RecapStore
import com.zenlauncher.zenmode.week.WeekReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * The one writer of [WidgetData]. It reads the stores the app already keeps (usage, the Zen
 * Score history, the recap, the gold ledger, the cached circle, the Quiet session) and nothing
 * else: no network, no new source of truth.
 *
 * Every widget update in a refresh pass asks at once, so the snapshot is kept for a few seconds
 * and they all draw the same one. Reading it also records today's Zen Score
 * ([ZenScoreStore.refresh]), which keeps the weekly streak's history current while the app
 * itself is closed.
 */
object WidgetDataLoader {

    private const val SHARE_MILLIS = 5_000L
    private const val KEY_POKE_DAY = "zeno_poke_day"
    private const val KEY_POKES = "zeno_pokes"
    private const val KEY_POKE_AT = "zeno_poke_at"

    /** How long Zeno keeps answering a poke before going back to the moment's line. */
    val ANSWER_MILLIS: Long = TimeUnit.SECONDS.toMillis(6)

    @Volatile private var cached: WidgetData? = null

    /** Drops the shared snapshot so the next read sees what just changed. */
    fun invalidate() {
        cached = null
    }

    @Synchronized
    fun load(context: Context, now: Long = System.currentTimeMillis()): WidgetData {
        cached?.takeIf { now - it.now in 0 until SHARE_MILLIS }?.let { return it }
        return build(context.applicationContext, now).also { cached = it }
    }

    private fun build(ctx: Context, now: Long): WidgetData {
        val zone = ZoneId.systemDefault()
        val at = Instant.ofEpochMilli(now).atZone(zone)
        val today = at.toLocalDate()
        val part = DayPart.at(at.hour, at.minute)
        val repository = UsageRepository(ctx, ServiceLocator.analyticsManager)
        val access = UsageAccess.isGranted(ctx)
        val usage = if (access) repository.getTodayUsage() else null
        val scores = ZenScoreStore(ctx, repository)
        val todayScore = usage?.let { scores.refresh(it, today) }
        val promiseHours = PromisePreferences.getDailyHours(ctx)
        val recorded = RecapStore(ctx).days()
        val minutesToday = (usage?.screenTimeInMillis ?: 0L) / 60_000L

        val week = WeekReader.read(scores.history(), recorded, today, todayScore, part)
        val gold = ZenGoldPromise.weekly(recorded, minutesToday, promiseHours, today).let { w ->
            val paise = GoldLedger.orders(ctx).values.sum()
            GoldState(
                balanceLabel = GoldLedger.balanceLabel(ctx),
                invested = paise > 0,
                open = w.goalMet,
                kept = w.unitsKept,
                outOfReach = w.goalOutOfReach
            )
        }
        val prefs = ctx.zenPrefs()
        val pokes = if (prefs.getString(KEY_POKE_DAY, null) == today.toString()) prefs.getInt(KEY_POKES, 0) else 0
        val answering = now - prefs.getLong(KEY_POKE_AT, 0L) in 0 until ANSWER_MILLIS

        return WidgetData(
            now = now,
            today = today,
            part = part,
            usageAccess = access,
            week = week,
            todayScore = todayScore,
            yesterdayScore = scores.scoreOn(today.minusDays(1)),
            minutesToday = minutesToday,
            promiseHours = promiseHours,
            quiet = QuietState(
                available = QuietSession.isAvailable(ctx),
                running = QuietSession.running(ctx, now),
                ended = QuietSession.justEnded(ctx, now, today)
            ),
            gold = gold,
            circle = circle(repository, today, zone),
            lastSevenMinutes = (6 downTo 1).map { recorded[today.minusDays(it.toLong())]?.screenTimeMinutes } + minutesToday,
            pokes = pokes,
            answering = answering
        )
    }

    private fun circle(repository: UsageRepository, today: LocalDate, zone: ZoneId): CircleState {
        val signedIn = runCatching { ServiceLocator.authProvider.isSignedIn() }.getOrDefault(false)
        if (!signedIn) return CircleState.SignedOut
        val circle = repository.getCachedCircle() ?: return CircleState.NoCircle
        val me = repository.getUserUid() ?: ServiceLocator.authProvider.getCurrentUserId()
        return CircleState.rank(circle.name, circle.members, me, today, zone)
    }

    /** A poke: Zeno answers for [ANSWER_MILLIS], and the next poke gets the next reply. */
    fun poke(context: Context, now: Long = System.currentTimeMillis()) {
        val prefs = context.zenPrefs()
        val today = LocalDate.now().toString()
        val count = if (prefs.getString(KEY_POKE_DAY, null) == today) prefs.getInt(KEY_POKES, 0) else 0
        prefs.edit()
            .putString(KEY_POKE_DAY, today)
            .putInt(KEY_POKES, count + 1)
            .putLong(KEY_POKE_AT, now)
            .apply()
        invalidate()
    }
}
