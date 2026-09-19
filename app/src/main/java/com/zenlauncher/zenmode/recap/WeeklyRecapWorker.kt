package com.zenlauncher.zenmode.recap

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Every few hours: record finished days before Android forgets them, and once a week
 * (Monday from [ANNOUNCE_AT]) announce last week's recap.
 */
class WeeklyRecapWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val store = RecapStore(applicationContext)
            val repository = UsageRepository(applicationContext, ServiceLocator.analyticsManager)
            val added = RecapCollector(applicationContext, repository, store).backfill()
            Log.i(TAG, "Backfilled $added day(s)")
            announceIfDue(store)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Recap work failed", e)
            Result.retry()
        }
    }

    private fun announceIfDue(store: RecapStore) {
        val now = LocalDateTime.now()
        val lastWeek = weekStartOf(now.toLocalDate()).minusWeeks(1)
        if (!isAnnounceTime(now) || store.isAnnounced(lastWeek)) return
        val recap = store.recap(lastWeek) ?: return
        store.markAnnounced(lastWeek)
        ServiceLocator.analyticsTracker.trackRecapReady(lastWeek.toString(), recap.outcome.analyticsKey)
        RecapNotifier.notifyReady(applicationContext, recap)
    }

    companion object {
        private const val TAG = "ZenRecap"
        private const val PERIODIC_WORK = "zen_weekly_recap"
        private const val CATCH_UP_WORK = "zen_weekly_recap_catch_up"
        private val ANNOUNCE_AT: LocalTime = LocalTime.of(8, 0)

        /** A new week has started and it's late enough on Monday (or any later day). */
        internal fun isAnnounceTime(now: LocalDateTime): Boolean =
            now.dayOfWeek != DayOfWeek.MONDAY || !now.toLocalTime().isBefore(ANNOUNCE_AT)

        /** Idempotent; call at every app start. */
        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WeeklyRecapWorker>(6, TimeUnit.HOURS).build()
            )
            // Periodic work may not run for hours after install; catch up once now.
            wm.enqueueUniqueWork(
                CATCH_UP_WORK,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<WeeklyRecapWorker>().build()
            )
        }
    }
}

internal object RecapNotifier {
    private const val CHANNEL_ID = "weekly_recap"
    private const val NOTIFICATION_ID = 4_107

    fun notifyReady(context: Context, recap: WeeklyRecap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return // The home screen opens the recap on the next visit instead.
        }
        ensureChannel(context)
        val text = when (recap.outcome) {
            RecapOutcome.KEPT -> "You kept your promise ${recap.daysKept} of 7 days. Invest is unlocked."
            RecapOutcome.MISSED -> "${recap.daysKept} of 7 days. See what got in the way, and win the next one."
        }
        val tap = PendingIntent.getActivity(
            context,
            recap.weekStart.hashCode(),
            RecapActivity.intent(context, recap.weekStart, RecapActivity.SOURCE_NOTIFICATION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_zen_mark_gradient)
            .setContentTitle("Your week in Zen is ready")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Weekly recap", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Your week in Zen, every Monday morning"
            }
        )
    }
}
