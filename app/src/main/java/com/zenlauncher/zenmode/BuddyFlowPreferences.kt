package com.zenlauncher.zenmode

import android.content.Context

/** Which buddy UI a user is on: the new Zen Circle flow, or the classic Zen Buddy summary. */
enum class BuddyFlow { ZEN_CIRCLE, ZEN_BUDDY_CLASSIC }

/**
 * Caches the one-time Zen Buddy -> Zen Circle migration choice. Null means "not decided yet" -
 * only possible for a user who already had a buddy relationship before this was introduced;
 * everyone else is auto-decided into [BuddyFlow.ZEN_CIRCLE] the first time it's checked.
 */
object BuddyFlowPreferences {
    private const val PREFS_NAME = "zenmode_prefs"
    private const val KEY_DECISION = "buddy_flow_decision"

    fun decision(context: Context): BuddyFlow? {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DECISION, null)
            ?: return null
        return runCatching { BuddyFlow.valueOf(stored) }.getOrNull()
    }

    fun setDecision(context: Context, flow: BuddyFlow) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DECISION, flow.name)
            .apply()
    }
}
