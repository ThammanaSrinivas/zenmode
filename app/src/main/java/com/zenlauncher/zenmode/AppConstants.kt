package com.zenlauncher.zenmode

object AppConstants {
    // The product's name wherever the app says it: "Welcome to $PRODUCT_NAME". ZenModeWordmark
    // draws its "OS" in the brand gradient wherever it appears in text. The only other copies
    // are in res/values/strings.xml (launcher label, accessibility description), which can't
    // reference Kotlin. scripts/check-duplicate-literals.sh fails on any other spelling.
    // ONE_SPELLING
    const val PRODUCT_NAME = "ZenMode OS"

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

    // BuddyStats has no real score/streak fields yet, so the buddy's side stays a
    // placeholder. Zen Scores are tenths (88 = 8.8), see ZenScore. Never wire these
    // to the signed-in user's own numbers, which come from ZenScoreStore.
    const val PLACEHOLDER_BUDDY_ZEN_SCORE = 88
    const val PLACEHOLDER_BUDDY_STREAK = 5

    // Zen Gold screen (Figma node 2026:1648) — the home screen's right-swipe page.
    // Promise-vs-screen-time tracking is real, on-device data now (see ZenGoldPromise.kt);
    // only the gold price forecast still needs a real Gold Streak backend (see
    // zenmode_core_private/docs/plans) that doesn't exist yet.
    const val PLACEHOLDER_PROMISE_HOURS = 4
    const val PLACEHOLDER_FORECAST_PERCENT = 20
    const val PLACEHOLDER_FORECAST_MONTHLY_AMOUNT = 200
    // The forecast card's 6-month axis and "today" marker are computed live from
    // LocalDate.now() (ZenGoldScreen.kt's ForecastCard) — no placeholder needed there.

    // My Promise screen (Figma node 2026:1793). The promise is chosen per week but
    // judged per day, so it steps in whole hours-per-day (7 hrs/week per tap).
    const val PROMISE_MIN_DAILY_HOURS = 1
    const val PROMISE_MAX_DAILY_HOURS = 12
    const val PROMISE_DAYS_PER_WEEK = 7
    const val PROMISE_DAYS_TO_UNLOCK = 5

    // Daily check-in overlay (see ZenCheckIn / ZenCheckInOverlay). Two moments a day, both
    // drawn on Home: an evening card at a time the user picks in Settings, and a last-hour
    // nudge while the promise can still be saved.
    /**
     * The last-hour nudge only applies to promises **longer** than this. On a 1- or 2-hour
     * promise "an hour left" arrives almost as soon as the phone is picked up, so it would
     * be noise rather than a warning worth acting on.
     */
    const val CHECK_IN_LAST_HOUR_MIN_PROMISE_HOURS = 2
    /** How much promise counts as "the last hour". */
    const val CHECK_IN_LAST_HOUR_WINDOW_MINUTES = 60L

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
    // Step-by-step guide to redeeming a Pro promo code at Google Play checkout. Not under an
    // App Links pathPrefix, so it always opens in the browser.
    const val REDEEM_CODE_URL = "https://zenmodeos.com/redeem/"
    // Public product board: what's being built, what's being considered, and the form that
    // adds to it. Settings -> "Feature requests" is the only link to it from the app, so the
    // page and this constant are the pair to keep in step (hosting/public/board/index.html).
    const val FEATURE_BOARD_URL = "https://zenmodeos.com/board/"
    // Play Store listing. Same id="com.zenlauncher.zenmode" MainActivity/SettingsActivity
    // build from `packageName` for the in-app "Rate us" flow; hardcoded here since this
    // object has no Context. Doubles as the download CTA appended to every share-card's
    // share text (Zen Score / Zen Gold / Streaks / weekly story — see share/*Share.kt).
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.zenlauncher.zenmode&hl=en_IN"
}
