package com.zenlauncher.zenmode

object AppConstants {
    const val THRESHOLD_HAPPY_MINUTES = 120
    const val THRESHOLD_NEUTRAL_MINUTES = 210
    const val GOAL_UNLOCKS_COUNT = 60

    // Mindfulness Percentage Thresholds
    const val MINDFULNESS_HAPPY_MIN_PERCENT = 70
    const val MINDFULNESS_NEUTRAL_MIN_PERCENT = 40

    // Resistence Screen
    const val COUNTDOWN_SECONDS = 7
    const val MAX_DAILY_SKIPS = 7

    // Worker
    const val STATS_SYNC_INTERVAL_MINUTES = 10

    // Random Connect
    const val RANDOM_CONNECT_COOLDOWN_MS = 30 * 1000L

    // v3 home screen placeholders. Stand-ins until the scoring and rewards
    // backend exists — swap these for real values, not the UI around them.
    const val PLACEHOLDER_ZEN_SCORE = 93
    const val PLACEHOLDER_GOLD_INVESTED = "2,350"
    const val PLACEHOLDER_GOLD_CHANGE_PERCENT = 38

    // Deliberately different from PLACEHOLDER_ZEN_SCORE — BuddyStats has no real
    // score/streak fields yet, and these must never be mistaken for (or accidentally
    // wired to) the signed-in user's own numbers above.
    const val PLACEHOLDER_BUDDY_ZEN_SCORE = 88
    const val PLACEHOLDER_BUDDY_STREAK = 5

    // Streaks milestone overlay (v3 redesign, Figma node 2026:2137). "Total mindful
    // days", the community percentile and the longest-streak stat all need real
    // streak-history tracking that doesn't exist yet — placeholders until it does.
    const val PLACEHOLDER_MILESTONE_DAYS = 120
    const val PLACEHOLDER_MILESTONE_PERCENTILE = 10
    const val PLACEHOLDER_MILESTONE_SCORE_THRESHOLD = 7
    const val PLACEHOLDER_LONGEST_STREAK_DAYS = 43
    const val PLACEHOLDER_LONGEST_STREAK_RANGE = "JUL 31–SEP 12"

    // Zen Gold screen (Figma node 2026:1648) — the home screen's right-swipe page.
    // Promise-vs-screen-time tracking and the price forecast both need a real Gold
    // Streak backend (see zenmode_core_private/docs/plans) that doesn't exist yet.
    const val PLACEHOLDER_PROMISE_HOURS = 4
    const val PLACEHOLDER_DAILY_AVERAGE_MINUTES = 59
    const val PLACEHOLDER_DAYS_UNTIL_UNLOCK = 3
    const val PLACEHOLDER_DAYS_LEFT_THIS_WEEK = 4
    // true = promise kept, false = broken, null = day hasn't happened yet
    val PLACEHOLDER_WEEKLY_PROMISE_STATUS: List<Boolean?> =
        listOf(true, false, false, true, true, null, null)
    const val PLACEHOLDER_FORECAST_PERCENT = 20
    const val PLACEHOLDER_FORECAST_MONTHLY_AMOUNT = 200
    const val PLACEHOLDER_FORECAST_TODAY_MONTH_INDEX = 2 // Sep, 0-based into the Jul-Dec axis
    const val PLACEHOLDER_INVEST_GOLD_UNLOCKED = false

    // External URLs
    const val GITHUB_URL = "https://github.com/ThammanaSrinivas/zenmode"
    const val YT_BUDDY_INVITE_URL = "https://youtu.be/48M1x2ryhpI"   // TODO: replace with actual YT link
    const val YT_BUDDY_CONFUSED_URL = "https://youtu.be/48M1x2ryhpI" // TODO: replace later
    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/zenmode-privacypolicy/zenmodeprivacy-policy"
}
