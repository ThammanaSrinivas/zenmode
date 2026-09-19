package com.zenlauncher.zenmode.onboarding

import com.zenlauncher.zenmode.BuildConfig

/** Copy and people shown on the welcome cover. Edit here, not in the composables. */
internal object OnboardingContent {

    data class Contributor(val name: String, val githubLogin: String?, val initials: String)

    /** Shout-outs, top contributor first. The welcome screen adds a "your name next" slot after them. */
    val contributors = listOf(
        Contributor("Srinivas", "ThammanaSrinivas", "SR"),
        Contributor("Kamal", "Kamal007OLica", "KS"),
        Contributor("Sharukhan", null, "SH")
    )

    data class Review(val quote: String, val author: String)

    /**
     * Play Store reviews for the welcome carousel. Must be verbatim, attributed reviews
     * from the live listing — until they're pasted in, release builds show only the
     * verified rating and debug builds show layout samples.
     */
    private val verifiedReviews: List<Review> = emptyList()

    private val layoutSamples = listOf(
        Review("I finally put my phone down before bed. My evenings feel like mine again.", "Sample review"),
        Review("My Zen Bro and I keep each other honest. It's oddly sweet.", "Sample review"),
        Review("Calm, beautiful home screen. I open Instagram half as much.", "Sample review")
    )

    val reviews: List<Review>
        get() = verifiedReviews.ifEmpty { if (BuildConfig.DEBUG) layoutSamples else emptyList() }

    data class Story(val eyebrow: String, val headline: String, val body: String)

    val stories = listOf(
        Story(
            eyebrow = "Zen Bro & Zen Gang",
            headline = "Quiet the noise, together!",
            body = "Pair up with a Zen Bro or bring your gang. Cheer each other on and keep each other honest."
        ),
        Story(
            eyebrow = "Zen Score",
            headline = "One number for a calmer day",
            body = "Your Zen Score rises when you're present and dips when you doomscroll. Updated live, all day."
        ),
        Story(
            eyebrow = "Zen Gold",
            headline = "Promise less screen. Earn gold.",
            body = "Keep your screen-time promise 5 of 7 days and Invest unlocks for the week."
        )
    )
}
