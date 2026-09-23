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

    // Zen Circle cap and reaction rate-limit constants live in core-api (Circle.kt /
    // UsageRepository.kt), not here — core-private's FirestoreDataSourceImpl needs the cap
    // for its join check, and core-private cannot depend on the app module.

    // v3 home screen placeholders. Stand-ins until the scoring and rewards
    // backend exists — swap these for real values, not the UI around them.
    const val PLACEHOLDER_GOLD_INVESTED = "0"
    // Only shown when PLACEHOLDER_GOLD_INVESTED is non-zero — see GoldOrder.changePercentFor,
    // which forces 0% for a ₹0 balance instead of this figure.
    const val PLACEHOLDER_GOLD_CHANGE_PERCENT = 38
    // "Days invested" stat on the Gold share overlay — needs the same Gold Streak backend
    // as the rest of the Zen Gold screen (see PLACEHOLDER_PROMISE_HOURS below).
    const val PLACEHOLDER_GOLD_DAYS_INVESTED = 120

    // BuddyStats has no real score/streak fields yet, so the buddy's side stays a
    // placeholder. Zen Scores are tenths (88 = 8.8), see ZenScore. Never wire these
    // to the signed-in user's own numbers, which come from ZenScoreStore.
    const val PLACEHOLDER_BUDDY_ZEN_SCORE = 88
    const val PLACEHOLDER_BUDDY_STREAK = 5

    // Zen Score share overlay's "RECLAIMED · X MINS (THIS MONTH)" stat — a monthly rollup
    // with no real data source yet (distinct from the daily session log on the Zen Score
    // screen, which is real — see SessionLogRepository in core-api).
    const val PLACEHOLDER_RECLAIMED_MINUTES = 1350

    // Streaks milestone overlay (v3 redesign, Figma node 2026:2137). Total mindful days and
    // the longest streak are now real (AppLogic.getTotalMindfulDays/getLongestStreak, from
    // RecapStore's real per-day history) — only the community percentile has no cross-user
    // data source yet, so it stays a placeholder.
    const val PLACEHOLDER_MILESTONE_PERCENTILE = 10
    /** Out of 10. A day's Zen Score clearing this counts as "mindful" for streaks/milestones. */
    const val MINDFUL_DAY_ZEN_SCORE_THRESHOLD = 7

    // Zen Gold screen (Figma node 2026:1648) — the home screen's right-swipe page.
    // Promise-vs-screen-time tracking and the price forecast both need a real Gold
    // Streak backend (see zenmode_core_private/docs/plans) that doesn't exist yet.
    const val PLACEHOLDER_PROMISE_HOURS = 4
    const val PLACEHOLDER_DAILY_AVERAGE_MINUTES = 59
    const val PLACEHOLDER_DAYS_UNTIL_UNLOCK = 3
    const val PLACEHOLDER_DAYS_LEFT_THIS_WEEK = 4
    // "Under Threshold & promise kept" state (Figma node 2026:1435) — days this
    // week that landed under the promise once the streak has already cleared.
    const val PLACEHOLDER_DAYS_CLEARED_UNDER = 6
    // true = promise kept, false = broken, null = day hasn't happened yet
    val PLACEHOLDER_WEEKLY_PROMISE_STATUS: List<Boolean?> =
        listOf(true, false, false, true, true, null, null)
    const val PLACEHOLDER_FORECAST_PERCENT = 20
    const val PLACEHOLDER_FORECAST_MONTHLY_AMOUNT = 200
    // The forecast card's 6-month axis and "today" marker are computed live from
    // LocalDate.now() (ZenGoldScreen.kt's ForecastCard) — no placeholder needed there.
    const val PLACEHOLDER_INVEST_GOLD_UNLOCKED = false

    // My Promise screen (Figma node 2026:1793). The promise is chosen per week but
    // judged per day, so it steps in whole hours-per-day (7 hrs/week per tap).
    const val PROMISE_MIN_DAILY_HOURS = 1
    const val PROMISE_MAX_DAILY_HOURS = 12
    const val PROMISE_DAYS_PER_WEEK = 7
    const val PROMISE_DAYS_TO_UNLOCK = 5

    // Invest Gold screen (Figma node 2026:1250), opened from Zen Gold's "Invest Gold".
    // Whole units only, capped per week by ZenMode (not by the broker). The instrument,
    // live price and linked demat need the brokerage integration that doesn't exist yet.
    const val INVEST_GOLD_MIN_UNITS = 1
    const val INVEST_GOLD_MAX_UNITS_PER_WEEK = 10
    val INVEST_GOLD_QUICK_PICK_UNITS: List<Int> = listOf(1, 3, 5)
    const val PLACEHOLDER_GOLD_SYMBOL = "GOLDBEES"
    const val PLACEHOLDER_GOLD_FUND_NAME = "Nippon India Gold ETF"
    const val PLACEHOLDER_GOLD_EXCHANGE = "NSE"
    const val PLACEHOLDER_GOLD_UNIT_PRICE_PAISE = 12_382L
    const val PLACEHOLDER_DEMAT_MASKED_ID = "ZD••••41"
    const val KITE_PACKAGE_NAME = "com.zerodha.kite3"
    const val KITE_WEB_URL = "https://kite.zerodha.com/"

    // External URLs
    const val GITHUB_URL = "https://github.com/ThammanaSrinivas/zenmode"
    const val SUPPORT_EMAIL = "helpdesk@zenmodeos.com"
    const val TELEGRAM_URL = "https://t.me/+Ka8sQAw_xwJhOGI1"
    const val PLAY_RATING = "4.6"                     // live Play listing, 2026-07-18 snapshot
    // Buddy invite links. Path must match the pathPrefix in AndroidManifest.xml's
    // App Links intent-filter and the /b/ route on the zenmodeos.com Firebase Hosting site.
    const val BUDDY_INVITE_BASE_URL = "https://zenmodeos.com/b/"
    // Zen Circle invite links -- separate path from Buddy above, same App Links /
    // Hosting pairing requirement, and a circle's Firestore document ID doubles as
    // its invite code (no separate invite_code field, see the plan doc).
    const val CIRCLE_INVITE_BASE_URL = "https://zenmodeos.com/c/"
    const val YT_BUDDY_INVITE_URL = "https://youtu.be/48M1x2ryhpI"   // TODO: replace with actual YT link
    const val YT_BUDDY_CONFUSED_URL = "https://youtu.be/48M1x2ryhpI" // TODO: replace later
    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/zenmode-privacypolicy/zenmodeprivacy-policy"
    const val TERMS_OF_SERVICE_URL = "https://zenmodeos.com/terms"
    // Play Store listing. Same id="com.zenlauncher.zenmode" MainActivity/SettingsActivity
    // build from `packageName` for the in-app "Rate us" flow; hardcoded here since this
    // object has no Context. Doubles as the download CTA appended to every share-card's
    // share text (Zen Score / Zen Gold / Streaks — see HomeShareOverlays.kt, HomeScreen.kt).
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.zenlauncher.zenmode&hl=en_IN"
}
