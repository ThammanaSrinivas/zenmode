package com.zenlauncher.zenmode.island.ui

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.island.CallItem
import com.zenlauncher.zenmode.island.ChargingPeek
import com.zenlauncher.zenmode.island.IslandAction
import com.zenlauncher.zenmode.island.IslandDevice
import com.zenlauncher.zenmode.island.IslandGeometry
import com.zenlauncher.zenmode.island.IslandHost
import com.zenlauncher.zenmode.island.IslandItem
import com.zenlauncher.zenmode.island.IslandPeek
import com.zenlauncher.zenmode.island.IslandPrefs
import com.zenlauncher.zenmode.island.IslandScene
import com.zenlauncher.zenmode.island.IslandStore
import com.zenlauncher.zenmode.island.IslandWindow
import com.zenlauncher.zenmode.island.LiveItem
import com.zenlauncher.zenmode.island.MediaItem
import com.zenlauncher.zenmode.island.MomentItem
import com.zenlauncher.zenmode.island.NotePeek
import com.zenlauncher.zenmode.island.QuietDonePeek
import com.zenlauncher.zenmode.island.QuietItem
import com.zenlauncher.zenmode.quiet.QuietSession
import com.zenlauncher.zenmode.ui.components.rememberReduceMotion
import com.zenlauncher.zenmode.ui.theme.IslandPalette
import com.zenlauncher.zenmode.widget.faces.WidgetAction
import com.zenlauncher.zenmode.widget.glance.ZenGlanceWidget
import java.time.LocalDate
import kotlin.math.abs

/** The island in its window: reads the store, sizes the window, and does what a tap asks. */
@Composable
fun IslandRoot(window: IslandWindow) {
    val scene by IslandStore.scene.collectAsState()
    val geometry by window.geometry
    val context = LocalContext.current
    val actions = remember { RootActions(context) }
    IslandSurface(
        scene = scene,
        geometry = geometry,
        lite = window.tier == IslandDevice.Tier.LITE,
        actions = actions,
        onTap = { id -> IslandStore.open(id); track("island_opened", id, "tap") },
        onSwipeUp = actions::swipeAway,
        onWindow = window::request
    )
}

/** What the island would like its window to be, in px, and whether it takes touches. */
fun interface WindowRequest {
    fun request(widthPx: Int, heightPx: Int, touchable: Boolean)
}

private data class FaceKey(val id: String, val open: Boolean)

/** How many faces the island keeps for cross-fades (the one showing and the ones fading out). */
private const val FACES_KEPT = 3

/**
 * The island itself, separate from its window so screenshot tests draw exactly what ships.
 *
 * One black shape morphs between the pill and the card on a spring (a cross-fade under reduced
 * motion). The content is laid out at its final size and revealed as the shape grows, so text
 * never reflows mid-animation. The window grows before the island does and shrinks only once it
 * has settled, so nothing is ever cut off and touches beside it still reach the app below.
 */
@Composable
fun IslandSurface(
    scene: IslandScene,
    geometry: IslandGeometry,
    lite: Boolean,
    actions: CardActions,
    onTap: (String) -> Unit,
    onSwipeUp: (Any) -> Unit,
    onWindow: WindowRequest,
    nowOverride: Long? = null
) {
    val density = LocalDensity.current
    val reduce = rememberReduceMotion()
    val haptics = LocalHapticFeedback.current
    val ticking = rememberNow(running = !scene.isEmpty && nowOverride == null)
    val now = nowOverride ?: ticking.value

    val shown: Any? = if (scene.isEmpty) null else scene.peek ?: scene.main
    val open = scene.open && shown != null
    val pillH = with(density) { geometry.pillHeight.toDp() }
    val gap = with(density) { geometry.cameraGap.toDp() }
    val cardW = with(density) { geometry.cardWidth.toDp() }
    val compact = shown?.let { compactFor(it, now, lite) }
    val pad = if (geometry.onCamera) 9.dp else 12.dp

    // The last few things shown, so a face fading out still has something to draw. Only a few:
    // the island runs all day, and a message's avatar is a bitmap.
    val last = remember { LinkedHashMap<String, Any>() }
    val key = shown?.let { FaceKey(scene.shownId ?: "", open) }
    if (key != null) {
        last.remove(key.id)
        last[key.id] = shown
        while (last.size > FACES_KEPT) last.remove(last.keys.first())
    }

    val targetW = when {
        shown == null -> if (geometry.onCamera) gap else pillH
        open -> cardW
        compact == null -> pillH
        else -> pillWidth(compact, gap, pad, geometry.onCamera)
    }
    val targetH = if (open) pillH + cardBodyHeight(shown!!) else pillH
    val targetR = if (open) 38.dp else pillH / 2

    val motion = spring<Dp>(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
    val w by animateDpAsState(targetW, if (reduce) snap() else motion, label = "island width")
    val h by animateDpAsState(targetH, if (reduce) snap() else motion, label = "island height")
    val r by animateDpAsState(targetR, if (reduce) snap() else motion, label = "island corner")
    val shownAlpha by animateFloatAsState(if (shown == null) 0f else 1f, tween(if (shown == null) 260 else 120), label = "island alpha")
    val shadowAlpha by animateFloatAsState(if (open) 1f else 0f, tween(220), label = "island shadow")

    val bubble = scene.bubble.takeIf { !open && scene.peek == null && shown != null }
    val bubbleGap = 6.dp

    // ── The window follows ──────────────────────────────────────────
    val lastW = remember { mutableIntStateOf(0) }
    val lastH = remember { mutableIntStateOf(0) }
    val settled = abs((w - targetW).value) < 0.5f && abs((h - targetH).value) < 0.5f
    val halfTarget = max(targetW / 2, if (bubble != null) targetW / 2 + bubbleGap + pillH else 0.dp)
    val needW = with(density) { (halfTarget * 2).roundToPx() }
    val needH = with(density) { (targetH + if (open) 26.dp else 2.dp).roundToPx() }
    val reqW = if (settled) needW else maxOf(lastW.intValue, needW)
    val reqH = if (settled) needH else maxOf(lastH.intValue, needH)
    SideEffect {
        lastW.intValue = reqW
        lastH.intValue = reqH
        onWindow.request(reqW, reqH, shown != null)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val shape = RoundedCornerShape(r)
        Box(
            Modifier
                .size(w, h)
                .alpha(shownAlpha)
                .shadow(18.dp * shadowAlpha, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(IslandPalette.island)
                .then(if (open) Modifier.border(1.dp, IslandPalette.edge, shape) else Modifier)
                .semantics { contentDescription = compact?.description?.let { "Zen Island. $it" } ?: "Zen Island" }
                .pointerInput(key) {
                    detectTapGestures(
                        onTap = {
                            val k = key ?: return@detectTapGestures
                            if (k.open) {
                                actions.touched()
                                shown?.let(actions::open)
                            } else if (shown !is ChargingPeek) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTap(k.id)
                            }
                        },
                        onLongPress = { key?.takeIf { !it.open }?.let { onTap(it.id) } }
                    )
                }
                .pointerInput(key) {
                    var dragged = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { if (dragged < -28.dp.toPx()) shown?.let(onSwipeUp) },
                        onVerticalDrag = { change, dy -> dragged += dy; change.consume() }
                    )
                }
        ) {
            AnimatedContent(
                targetState = key,
                transitionSpec = {
                    if (reduce) {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(80))
                    } else {
                        (fadeIn(tween(200, delayMillis = 80)) + scaleIn(tween(260, delayMillis = 40), initialScale = 0.92f)) togetherWith
                            fadeOut(tween(90))
                    }
                },
                contentAlignment = Alignment.TopCenter,
                label = "island face"
            ) { k ->
                if (k == null) return@AnimatedContent
                val item = last[k.id] ?: return@AnimatedContent
                if (k.open) {
                    Box(Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).requiredWidth(cardW)) {
                        Column {
                            Card(item, pillH, now, lite, actions,
                                avoidStart = with(density) { geometry.avoidStart.toDp() }, avoidEnd = with(density) { geometry.avoidEnd.toDp() })
                        }
                    }
                } else {
                    val c = compactFor(item, now, lite) ?: return@AnimatedContent
                    // Round a camera, both sides match so it stays in the middle; elsewhere the pill is as narrow as it can be.
                    val lead = if (geometry.onCamera) max(c.leadWidth, c.trailWidth) else c.leadWidth
                    val trail = if (geometry.onCamera) lead else c.trailWidth
                    Row(
                        Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).requiredSize(pillWidth(c, gap, pad, geometry.onCamera), pillH)
                            .padding(horizontal = pad),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(lead), contentAlignment = Alignment.CenterStart) { c.lead() }
                        Spacer(Modifier.width(gap))
                        Box(Modifier.width(trail), contentAlignment = Alignment.CenterEnd) { c.trail() }
                    }
                }
            }
        }

        // The bubble: what else is live, detached beside the pill, as on iPhone.
        if (bubble != null) {
            Box(
                Modifier
                    .offset(x = w / 2 + bubbleGap + pillH / 2)
                    .size(pillH)
                    .alpha(shownAlpha)
                    .clip(CircleShape)
                    .background(IslandPalette.island)
                    .pointerInput(bubble.id) { detectTapGestures(onTap = { onTap(bubble.id) }) },
                contentAlignment = Alignment.Center
            ) {
                BubbleFace(bubble, now, pillH)
            }
        }
    }
}

/** The pill's width for [c]: symmetric round a camera, as narrow as the content elsewhere. */
private fun pillWidth(c: Compact, gap: Dp, pad: Dp, onCamera: Boolean): Dp =
    if (onCamera) gap + (max(c.leadWidth, c.trailWidth) + pad) * 2 else c.leadWidth + gap + c.trailWidth + pad * 2

private fun track(event: String, id: String, source: String) = runCatching {
    // Kinds only: never what a notification or a song says.
    ServiceLocator.analyticsManager.trackEvent(event, mapOf("state" to id.substringBefore(':'), "source" to source))
}

/** `island_action {state, action}`: which kind of thing, and what was done with it. */
private fun action(id: String, what: String) = runCatching {
    ServiceLocator.analyticsManager.trackEvent("island_action", mapOf("state" to id.substringBefore(':'), "action" to what))
}

/** What the card's buttons do, on the phone. */
private class RootActions(private val context: Context) : CardActions {

    override fun open(item: Any) {
        when (item) {
            is NotePeek -> {
                send(item.open)
                IslandStore.dismiss()
            }
            is CallItem -> send(item.open).also { IslandStore.close() }
            is LiveItem -> send(item.open).also { IslandStore.close() }
            is MediaItem -> IslandHost.media()?.open().also { IslandStore.close() }
            is QuietItem, is QuietDonePeek -> start(WidgetAction.OPEN_QUIET_SETUP)
            else -> start(WidgetAction.OPEN_TODAY)
        }
        action((item as? IslandItem)?.id ?: (item as? IslandPeek)?.id ?: "other", "open")
    }

    override fun send(action: IslandAction) {
        send(action.intent)
        IslandStore.dismiss()
    }

    override fun endQuiet() {
        QuietSession.end(context)
        IslandStore.close()
        action(QuietItem.ID, "end")
    }

    override fun pet() {
        IslandHost.poke()
        action(MomentItem.ID, "pet")
    }

    override fun mediaToggle() {
        IslandHost.media()?.toggle()
        IslandStore.touch()
    }

    override fun mediaNext() {
        IslandHost.media()?.next()
        IslandStore.touch()
    }

    override fun mediaPrevious() {
        IslandHost.media()?.previous()
        IslandStore.touch()
    }

    override fun touched() = IslandStore.touch()

    /** Swiped up: a passing thing goes; the week's moment stays away until tomorrow. */
    fun swipeAway(item: Any) {
        if (item is MomentItem) {
            IslandPrefs.dismissMoment(context, LocalDate.now())
            IslandStore.removeLive(MomentItem.ID)
        } else {
            IslandStore.dismiss()
        }
    }

    private fun start(action: WidgetAction) {
        runCatching { context.startActivity(ZenGlanceWidget.intentFor(context, action)) }
        IslandStore.close()
    }

    /** Opens what a notification points at, with the tap's permission to start an activity. */
    private fun send(intent: PendingIntent?) {
        if (intent == null) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                intent.send(context, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
        }.onFailure { if (com.zenlauncher.zenmode.BuildConfig.DEBUG) android.util.Log.w("ZenIsland", "couldn't send ${intent.creatorPackage}", it) }
    }
}
