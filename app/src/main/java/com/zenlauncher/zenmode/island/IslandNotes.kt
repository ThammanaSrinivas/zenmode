package com.zenlauncher.zenmode.island

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import android.util.Log
import com.zenlauncher.zenmode.BuildConfig
import com.zenlauncher.zenmode.ContentBlockPrefs

/**
 * Turns notifications into island things: a call or another app's live activity stays, a
 * message peeks, and everything [NoteRules] turns away is left alone. Called from
 * [com.zenlauncher.zenmode.ZenNotificationListenerService] on the main thread, and only does
 * work while the island is running.
 *
 * Nothing read here leaves the phone or is stored: the island holds a notification only while
 * it shows it.
 */
object IslandNotes {

    private const val TAG = "ZenIsland"
    private const val ICON_DP = 40
    /** Keys already seen, so an "alert once" notification updating itself does not peek again. */
    private val seen = LinkedHashSet<String>()
    private const val SEEN_MAX = 200
    private val icons = LruCache<String, Bitmap>(24)

    /** Android 16's promoted-ongoing flag, read by name so older SDKs still compile and run. */
    private val FLAG_PROMOTED: Int by lazy {
        runCatching { Notification::class.java.getField("FLAG_PROMOTED_ONGOING").getInt(null) }.getOrDefault(0)
    }

    fun posted(service: NotificationListenerService, sbn: StatusBarNotification, ranking: NotificationListenerService.RankingMap?) {
        if (!IslandHost.isActive) return
        val n = sbn.notification ?: return
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = lastMessage(n) ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val rank = NotificationListenerService.Ranking()
        val ranked = ranking?.getRanking(sbn.key, rank) == true
        val alertOnce = n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val facts = NoteRules.Facts(
            pkg = sbn.packageName,
            own = sbn.packageName == service.packageName,
            category = n.category,
            ongoing = sbn.isOngoing,
            groupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            importance = if (ranked) rank.importance else NotificationManager.IMPORTANCE_DEFAULT,
            passesDnd = !ranked || rank.matchesInterruptionFilter(),
            chronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER),
            promoted = FLAG_PROMOTED != 0 && n.flags and FLAG_PROMOTED != 0,
            determinateProgress = progressMax > 0 && !extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE),
            mediaStyle = extras.containsKey(Notification.EXTRA_MEDIA_SESSION),
            foregroundService = n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            quietUpdate = alertOnce && sbn.key in seen,
            hasWords = title.isNotBlank() || text.isNotBlank()
        )
        remember(sbn.key)
        val ctx = NoteRules.Context(
            peeksOn = IslandPrefs.notesOn(service),
            liveOn = IslandPrefs.liveOn(service),
            quiet = IslandStore.liveItem(QuietItem.ID) != null,
            foregroundPkg = null,
            quietedApps = ContentBlockPrefs.quietedApps(service)
        )
        var route = NoteRules.route(facts, ctx)
        // Only look up the app on screen when it could change the answer (it costs a query).
        if (route == NoteRules.Route.PEEK || route == NoteRules.Route.HOLD) {
            route = NoteRules.route(facts, ctx.copy(foregroundPkg = foregroundApp(service)))
        }
        // Debug builds only, and never the words: which way a notification went, and why.
        if (BuildConfig.DEBUG) Log.d(TAG, "${sbn.packageName} → $route")
        // One notification is one thing in the island: an update that changed what it is (a plain
        // ongoing notification that became a call, a call that ended) replaces what it was.
        if (route != NoteRules.Route.CALL) IslandStore.removeLive("call:${sbn.key}")
        if (route != NoteRules.Route.LIVE) IslandStore.removeLive("live:${sbn.key}")
        when (route) {
            NoteRules.Route.CALL -> IslandStore.putLive(
                CallItem(
                    key = sbn.key,
                    name = title.ifBlank { appLabel(service, sbn.packageName) },
                    since = n.`when`.takeIf { it > 0 } ?: sbn.postTime,
                    open = n.contentIntent,
                    actions = actions(n),
                    incoming = extras.getInt(EXTRA_CALL_TYPE, 0) == CALL_TYPE_INCOMING
                )
            )
            NoteRules.Route.LIVE -> IslandStore.putLive(
                LiveItem(
                    key = sbn.key,
                    pkg = sbn.packageName,
                    appLabel = appLabel(service, sbn.packageName),
                    icon = appIcon(service, sbn.packageName),
                    title = title,
                    text = text,
                    chronoBase = if (facts.chronometer) n.`when` else null,
                    countDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN),
                    progress = if (facts.determinateProgress) extras.getInt(Notification.EXTRA_PROGRESS, 0).toFloat() / progressMax else null,
                    open = n.contentIntent,
                    actions = actions(n)
                )
            )
            NoteRules.Route.PEEK -> {
                val hide = isLockedAndPrivate(service, n)
                val public = n.publicVersion?.extras
                IslandStore.post(
                    NotePeek(
                        key = sbn.key,
                        pkg = sbn.packageName,
                        appLabel = appLabel(service, sbn.packageName),
                        icon = if (facts.own) null else appIcon(service, sbn.packageName),
                        avatar = if (hide) null else largeIcon(service, n),
                        own = facts.own,
                        title = if (hide) public?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: appLabel(service, sbn.packageName) else title,
                        text = if (hide) public?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: LOCKED_TEXT else text,
                        at = sbn.postTime,
                        open = n.contentIntent,
                        actions = if (hide) emptyList() else actions(n),
                        opens = NoteRules.opens(facts.importance)
                    )
                )
            }
            NoteRules.Route.HOLD -> IslandStore.hold()
            NoteRules.Route.IGNORE -> Unit
        }
    }

    fun removed(sbn: StatusBarNotification) {
        IslandStore.removeLive("call:${sbn.key}")
        IslandStore.removeLive("live:${sbn.key}")
        seen.remove(sbn.key)
    }

    /** On connect: calls and timers already running come in; nothing old peeks. */
    fun scan(service: NotificationListenerService) {
        val active = runCatching { service.activeNotifications }.getOrNull() ?: return
        val ranking = runCatching { service.currentRanking }.getOrNull()
        active.forEach { sbn ->
            if (sbn.isOngoing) posted(service, sbn, ranking) else remember(sbn.key)
        }
    }

    private const val LOCKED_TEXT = "New notification"
    private const val EXTRA_CALL_TYPE = "android.callType"
    private const val CALL_TYPE_INCOMING = 1

    private fun remember(key: String) {
        seen.remove(key)
        seen.add(key)
        while (seen.size > SEEN_MAX) seen.remove(seen.first())
    }

    /** The newest line of a conversation, if it is one. */
    private fun lastMessage(n: Notification): String? {
        val messages = n.extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
        val last = messages.lastOrNull() as? android.os.Bundle ?: return null
        val sender = last.getCharSequence("sender")?.toString()
        val body = last.getCharSequence("text")?.toString() ?: return null
        val group = n.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE) != null
        return if (group && !sender.isNullOrBlank()) "$sender: $body" else body
    }

    /** Up to two of the notification's own actions (reply needs typing, so it is left out). */
    private fun actions(n: Notification): List<IslandAction> =
        n.actions.orEmpty()
            .filter { it.remoteInputs.isNullOrEmpty() && !it.title.isNullOrBlank() }
            .take(2)
            .map { IslandAction(it.title.toString(), it.actionIntent) }

    /** A locked phone with a secure lock shows only what the app chose to show when locked. */
    private fun isLockedAndPrivate(ctx: Context, n: Notification): Boolean {
        val km = ctx.getSystemService(KeyguardManager::class.java) ?: return false
        return km.isKeyguardLocked && km.isDeviceSecure && n.visibility != Notification.VISIBILITY_PUBLIC
    }

    fun appLabel(ctx: Context, pkg: String): String = runCatching {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg.substringAfterLast('.'))

    fun appIcon(ctx: Context, pkg: String): Bitmap? {
        icons.get(pkg)?.let { return it }
        val px = (ICON_DP * ctx.resources.displayMetrics.density).toInt()
        val bmp = runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(px, px) }.getOrNull() ?: return null
        icons.put(pkg, bmp)
        return bmp
    }

    private fun largeIcon(ctx: Context, n: Notification): Bitmap? {
        val icon: Icon = n.getLargeIcon() ?: return null
        val px = (ICON_DP * ctx.resources.displayMetrics.density).toInt()
        return runCatching { icon.loadDrawable(ctx)?.toBitmap(px, px) }.getOrNull()
    }

    /** The app the person is in right now, from the last few seconds of usage events. */
    private fun foregroundApp(ctx: Context): String? = runCatching {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 60_000L, now)
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) last = e.packageName
        }
        last
    }.getOrNull()
}
