package com.zenlauncher.zenmode

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The home-screen gestures, each off until it's switched on in Settings → Gestures. Home
 * behaves exactly as it always did for anyone who never opens that group.
 *
 * [SWIPE_UP_SEARCH] is the free one, and the only gesture that takes something off the screen:
 * with it on, the search pill comes off Home, because the gesture *is* the way into search —
 * leaving both would keep the pill's 48dp for a control the gesture has replaced.
 *
 * The rest are Pro. A Pro gesture is ignored whenever Pro isn't active (see [active]) rather
 * than being erased, so a lapsed subscription can't leave a gesture firing that Settings shows
 * as locked, and resubscribing brings back the same set.
 */
enum class HomeGesture(val isPro: Boolean) {
    /** Swipe up anywhere on Home to open search. */
    SWIPE_UP_SEARCH(isPro = false),
    /** Tap the left or right margin to open the page that way — Zen Score or Zen Gold. */
    MARGIN_TAP_PAGES(isPro = true),
    /** Double-tap an empty part of Home to lock the phone. */
    DOUBLE_TAP_LOCK(isPro = true)
}

object GesturePreferences {
    private const val PREFS_NAME = "zenmode_prefs"

    @Volatile
    private var flow: MutableStateFlow<Set<HomeGesture>>? = null

    /** Every gesture switched on in Settings, Pro ones included. Pass through [active] to use. */
    fun state(context: Context): StateFlow<Set<HomeGesture>> = flow(context)

    fun isEnabled(context: Context, gesture: HomeGesture): Boolean =
        gesture in flow(context).value

    fun setEnabled(context: Context, gesture: HomeGesture, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(key(gesture), enabled).apply()
        flow(context).value = read(context)
    }

    /**
     * The gestures actually in force: everything switched on, minus the Pro ones when [isPro]
     * is false. The single place that filter happens, so Home and Settings can't disagree about
     * whether a locked gesture is live.
     */
    fun active(enabled: Set<HomeGesture>, isPro: Boolean): Set<HomeGesture> =
        if (isPro) enabled else enabled.filterTo(LinkedHashSet()) { !it.isPro }

    /** Re-reads the stored set; called when the prefs file is wiped out from under the cache. */
    internal fun reload(context: Context) {
        flow?.value = read(context)
    }

    private fun flow(context: Context): MutableStateFlow<Set<HomeGesture>> =
        flow ?: synchronized(this) {
            flow ?: MutableStateFlow(read(context)).also { flow = it }
        }

    private fun read(context: Context): Set<HomeGesture> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return HomeGesture.entries.filterTo(LinkedHashSet()) { prefs.getBoolean(key(it), false) }
    }

    private fun key(gesture: HomeGesture) = "gesture_${gesture.name.lowercase()}"
}
