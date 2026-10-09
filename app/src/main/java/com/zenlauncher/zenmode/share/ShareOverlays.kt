package com.zenlauncher.zenmode.share

import androidx.compose.runtime.Composable
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

// ── The three post-shaped share points ────────────────────────────
// Home's streak flame and gold row, and the Zen Score page's "Share Zen Score". Each is the one
// ShareSheet with its own card; the card itself (and so the picture, the words and the sound)
// is chosen by what the user has actually reached — see StreakShare, ScoreShare, GoldShare.

@Composable
fun StreakShareSheet(share: StreakShare, onDismiss: () -> Unit) = ShareSheet(
    eyebrow = "STREAKS",
    key = share,
    art = { StreakArt(it, share) },
    note = share.nextMilestone,
    shareLabel = "Share my streak",
    fileBaseName = "zenmode_streak_${share.days}",
    shareText = share.shareText,
    chooserTitle = "Share streak",
    analyticsKey = share.analyticsKey,
    onDismiss = onDismiss
)

@Composable
fun ZenScoreShareSheet(share: ScoreShare, onDismiss: () -> Unit) = ShareSheet(
    eyebrow = "ZEN SCORE",
    key = share,
    art = { ScoreArt(it, share) },
    note = share.note,
    shareLabel = "Share my Zen Score",
    fileBaseName = "zenmode_zen_score",
    shareText = share.shareText,
    chooserTitle = "Share Zen Score",
    analyticsKey = share.analyticsKey,
    onDismiss = onDismiss
)

@Composable
fun ZenGoldShareSheet(share: GoldShare, onDismiss: () -> Unit) = ShareSheet(
    eyebrow = "ZEN GOLD",
    key = share,
    art = { GoldArt(it, share) },
    note = share.nextStep,
    shareLabel = "Share my Zen Gold",
    fileBaseName = "zenmode_zen_gold",
    shareText = share.shareText,
    chooserTitle = "Share Zen Gold",
    analyticsKey = share.analyticsKey,
    onDismiss = onDismiss
)

/**
 * Logs a card leaving the app through the existing referral event: [channel] is how
 * (share_image, save_image, share_clip, save_clip), [card] which card and tier.
 */
internal fun trackShare(channel: String, card: String) {
    if (ServiceLocator.isInitialized) ServiceLocator.analyticsTracker.trackReferralShareInitiated(channel, card)
}
