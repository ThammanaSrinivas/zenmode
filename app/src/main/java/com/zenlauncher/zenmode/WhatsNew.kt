package com.zenlauncher.zenmode

/** One line of the "What's new" overlay: a short name and what it means for the user. */
data class WhatsNewItem(
    val title: String,
    val detail: String,
    /** Draws the row's dot in the reward hue instead of brand green. One per release, at most. */
    val isReward: Boolean = false
)

/**
 * The release notes shown once, on Home, after an update. To ship new notes: rewrite [items],
 * set [NOTES_VERSION_CODE] to the release's `versionCode`, and everyone below it sees them once.
 */
object WhatsNew {
    const val NOTES_VERSION_CODE = 16

    const val HEADLINE = "A fresh look and new ways to stay focused."

    val items = listOf(
        WhatsNewItem("All-new design", "New fonts, colours and a Home screen that follows your mood"),
        WhatsNewItem("Zen Check-In", "Check-ins and promise streaks keep you accountable"),
        WhatsNewItem("Zen Gold", "Weekly and monthly promises that unlock real rewards", isReward = true),
        WhatsNewItem("Zen Circle", "Connect with a random buddy in one tap"),
        WhatsNewItem("Share cards & weekly report", "Beautiful cards to share, and your week as a PDF"),
        WhatsNewItem("Smoother onboarding", "A first-run Home guide shows you around"),
        WhatsNewItem("Under the hood", "Stability, accessibility and performance fixes")
    )

    /**
     * Fresh installs never see notes for the version they installed (onboarding already shows
     * them around), so their first check records the current notes as seen. Everyone else sees
     * them once, while their last seen notes are older than [notesVersionCode].
     */
    fun shouldShow(seenVersionCode: Int?, isFreshInstall: Boolean, notesVersionCode: Int = NOTES_VERSION_CODE): Boolean =
        when {
            seenVersionCode != null -> seenVersionCode < notesVersionCode
            else -> !isFreshInstall
        }
}
