package com.zenlauncher.zenmode.share

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.recap.WeeklyRecap
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── Weekly share studio ───────────────────────────────────────────
// Where a week's story is shared from — the recap's share button and Settings → Weekly reports.
// The 9:16 story plays in the middle (tap it to play it again), and leaves as a clip with its
// soundtrack, or as the settled image; PRO keeps the PDF report one tap away. Making the clip
// takes a few seconds, so its button turns into the progress, and closing the studio stops it.

@Composable
fun WeeklyShareStudio(
    visible: Boolean,
    recap: WeeklyRecap?,
    canSharePdf: Boolean,
    onSharePdf: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    // The last week shown, so the studio can fade out even after the caller lets go of it.
    var shown by remember { mutableStateOf(recap) }
    if (recap != null) shown = recap
    AnimatedVisibility(visible = visible && shown != null, enter = fadeIn(tween(240)), exit = fadeOut(tween(180))) {
        shown?.let { StudioContent(it, canSharePdf, onSharePdf, onDismiss) }
    }
}

@Composable
private fun StudioContent(recap: WeeklyRecap, canSharePdf: Boolean, onSharePdf: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val feedback = rememberZenFeedback()
    val share = remember(recap) { WeeklyShare(recap) }
    val kit = rememberShareKit()
    val art = remember(kit, share) { WeeklyArt(kit, share) }
    val makeArt: (ShareKit) -> ShareArt = { WeeklyArt(it, share) }
    var playKey by remember { mutableIntStateOf(0) }
    var clip by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    val busy = clip != null

    fun makeClip(save: Boolean) {
        if (busy) return
        progress = 0f
        trackShare(if (save) "save_clip" else "share_clip", share.analyticsKey)
        clip = scope.launch {
            try {
                val done = if (save) {
                    ShareExport.saveClip(context, makeArt, share.fileBaseName) { progress = it }
                } else {
                    ShareExport.shareClip(context, makeArt, share.fileBaseName, share.shareText, "Share your week") { progress = it }
                }
                if (!done && !save) {
                    // Some phones can't encode video; the settled image still tells the week.
                    Toast.makeText(context, "Couldn't make a clip on this phone, so here's the image.", Toast.LENGTH_LONG).show()
                    ShareExport.shareImage(context, makeArt, share.fileBaseName, share.shareText, "Share your week")
                } else if (!done) {
                    Toast.makeText(context, "Couldn't save the clip. Please try again.", Toast.LENGTH_LONG).show()
                }
            } finally {
                clip = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.ink_base).copy(alpha = 0.94f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* consume */ }
            .systemBarsPadding()
            .padding(horizontal = 24.rdp, vertical = 12.rdp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SHARE YOUR WEEK",
                    fontFamily = DepartureMono,
                    fontSize = 13.rsp,
                    letterSpacing = 1.3.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .pressScale(onClick = {
                            clip?.cancel()
                            onDismiss()
                        }, onClickLabel = "Close"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val ratio = ShareFormat.STORY.width / ShareFormat.STORY.height.toFloat()
                val width = minOf(maxWidth, maxHeight * ratio)
                ShareArtView(
                    art = art,
                    playKey = playKey,
                    modifier = Modifier
                        .width(width)
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(22.rdp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            role = Role.Button,
                            onClickLabel = "Play the week again"
                        ) { playKey++ }
                )
            }

            Spacer(Modifier.height(8.rdp))
            Text(
                text = "TAP THE CARD TO PLAY IT AGAIN",
                fontFamily = DepartureMono,
                fontSize = 11.rsp,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.42f)
            )
            Spacer(Modifier.height(14.rdp))

            ClipButton(busy = busy, progress = progress) {
                feedback.shared()
                makeClip(save = false)
            }
            Spacer(Modifier.height(10.rdp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.rdp), modifier = Modifier.fillMaxWidth()) {
                GhostButton("Share image", enabled = !busy, modifier = Modifier.weight(1f)) {
                    feedback.shared()
                    trackShare("share_image", share.analyticsKey)
                    scope.launch { ShareExport.shareImage(context, makeArt, share.fileBaseName, share.shareText, "Share your week") }
                }
                GhostButton("Save clip", enabled = !busy, modifier = Modifier.weight(1f)) {
                    feedback.saved()
                    makeClip(save = true)
                }
            }
            if (canSharePdf) {
                Text(
                    text = "Send the PDF report",
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.rsp,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier
                        .padding(top = 6.rdp)
                        .clickable(role = Role.Button, enabled = !busy, onClick = onSharePdf)
                        .padding(vertical = 12.rdp, horizontal = 16.rdp)
                )
            }
        }
    }
}

/** "Share as clip" — while the clip is being made, the pill fills with its own progress. */
@Composable
private fun ClipButton(busy: Boolean, progress: Float, onClick: () -> Unit) {
    val green = colorResource(R.color.zen_700)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.rdp)
            .clip(RoundedCornerShape(50))
            .background(if (busy) green.copy(alpha = 0.35f) else green)
            .semantics { if (busy) contentDescription = "Making your clip, ${(progress * 100).roundToInt()} percent" }
            .clickable(enabled = !busy, role = Role.Button, onClickLabel = "Share as clip", onClick = onClick)
    ) {
        if (busy) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                    .background(green)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!busy) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.rdp))
                Spacer(Modifier.width(6.rdp))
            }
            Text(
                text = if (busy) "Making your clip… ${(progress * 100).roundToInt()}%" else "Share as clip",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 17.rsp,
                letterSpacing = (-0.35).sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GhostButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.rdp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Text(text = text, fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 16.rsp, color = Color.White)
    }
}
