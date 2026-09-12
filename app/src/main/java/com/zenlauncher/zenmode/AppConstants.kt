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

    // External URLs
    const val GITHUB_URL = "https://github.com/ThammanaSrinivas/zenmode"
    const val YT_BUDDY_INVITE_URL = "https://youtu.be/48M1x2ryhpI"   // TODO: replace with actual YT link
    const val YT_BUDDY_CONFUSED_URL = "https://youtu.be/48M1x2ryhpI" // TODO: replace later
    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/zenmode-privacypolicy/zenmodeprivacy-policy"
}
