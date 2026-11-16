package com.zenlauncher.zenmode.widget.faces

import android.graphics.Canvas
import android.graphics.RectF
import com.zenlauncher.zenmode.widget.WidgetData
import com.zenlauncher.zenmode.widget.art.Backdrop
import com.zenlauncher.zenmode.widget.art.Surface
import com.zenlauncher.zenmode.widget.art.SurfacePainter
import com.zenlauncher.zenmode.widget.art.WidgetKit
import com.zenlauncher.zenmode.coreapi.week.MomentAccent
import com.zenlauncher.zenmode.widget.art.WidgetLight
import com.zenlauncher.zenmode.ui.theme.WidgetTokens

/** What a tap on a widget, or on one part of it, does. */
enum class WidgetAction {
    OPEN_TODAY, OPEN_WEEK, OPEN_GOLD, OPEN_CIRCLE, OPEN_QUIET_SETUP, OPEN_USAGE_ACCESS, OPEN_SIGN_IN,
    /** Tap Zeno: he answers. Runs in place, without opening the app. */
    POKE,
    /** Start and end a Quiet session in place, without opening the app. */
    QUIET_START, QUIET_END
}

/** Everything a face is drawn from. Sizes in dp. */
class FaceInput(
    val data: WidgetData,
    val w: Float,
    val h: Float,
    /** The phone is in dark theme. */
    val dark: Boolean,
    val backdrop: Backdrop,
    /** The system's widget corner radius. */
    val radius: Float,
    /** On the lock screen: score and good days only. No minutes, no circle, no gold. */
    val private: Boolean = false,
    /** The week's 168 hours, for the bloom; null when the face doesn't need them. */
    val bloom: List<BloomCell>? = null
)

/** A tappable part of a face, in dp. */
data class Hit(val rect: RectF, val action: WidgetAction)

/**
 * The Quiet countdown: drawn by the host as a system Chronometer over the face, so it ticks
 * every second with no updates from us. [endsAt] is wall-clock time.
 */
data class Clock(val x: Float, val y: Float, val w: Float, val h: Float, val endsAt: Long, val size: Float, val color: Int)

/** What drawing a face produced besides the pixels. */
class FaceResult(
    val description: String,
    val tap: WidgetAction,
    val hits: List<Hit>,
    val clock: Clock?,
    val surface: Surface
)

/** One widget's face: draws itself for any size, from 1 × 1 to 4 × 4. */
interface Face {
    fun draw(scope: FaceScope): FaceResult
}

/** The canvas and the size, plus the helpers every face shares. */
class FaceScope(val kit: WidgetKit, val canvas: Canvas, val input: FaceInput, private val pxPerDp: Float = 1f) {
    val w: Float get() = input.w
    val h: Float get() = input.h
    val data: WidgetData get() = input.data

    /** One row (≈102dp), two (≈220), three (≈338) or four (≈456). */
    val rows: Int get() = when {
        h < 150 -> 1
        h < 290 -> 2
        h < 400 -> 3
        else -> 4
    }

    /** Four columns or more (≈276dp and up on a five-column launcher). */
    val wide: Boolean get() = w >= 250

    /** Two columns on a five-column launcher (≈130dp). */
    val narrow: Boolean get() = w < 160

    private val hits = mutableListOf<Hit>()
    private var clock: Clock? = null

    lateinit var surface: Surface
        private set

    val t: WidgetTokens get() = surface.tokens

    /** Paints the ground. Call first; [light] is ignored for Glass and Plain. */
    fun ground(surface: Surface, light: WidgetLight.Spec?) {
        this.surface = surface
        SurfacePainter.ground(canvas, w, h, input.radius, surface, input.backdrop, light, pxPerDp)
    }

    fun hit(rect: RectF, action: WidgetAction): RectF {
        hits += Hit(RectF(rect), action)
        return rect
    }

    fun clock(c: Clock) {
        clock = c
    }

    fun result(description: String, tap: WidgetAction) = FaceResult(description, tap, hits.toList(), clock, surface)

    /** The moment's hue, as a glow. */
    val momentHue: WidgetLight.Hue
        get() = when (data.moment.accent) {
            MomentAccent.ZEN -> WidgetLight.Hue.ZEN
            MomentAccent.AMBER -> WidgetLight.Hue.AMBER
            MomentAccent.EMBER -> WidgetLight.Hue.EMBER
            MomentAccent.DUSK -> WidgetLight.Hue.DUSK
        }

    /**
     * The light for this widget: today's wash, the time of day, and a glow of [hue] centred on
     * the hero at ([glowX], [glowY]). [starBox] places one star a won week, on dark skies.
     */
    fun lit(
        glowX: Float? = null,
        glowY: Float? = null,
        glowRadius: Float = maxOf(w, h) * 0.5f,
        hue: WidgetLight.Hue = momentHue,
        celebration: Boolean = data.moment.kind.isCelebration,
        mood: WidgetLight.Mood = data.mood,
        starBox: WidgetLight.Box? = null,
        moon: WidgetLight.Moon? = null
    ): WidgetLight.Spec {
        val weeks = data.moment.streakWeeks
        return WidgetLight.Spec(
            w = w, h = h, mood = mood, part = data.part,
            glow = if (glowX != null && glowY != null) WidgetLight.Glow(hue, glowX, glowY, glowRadius * if (celebration) 1.3f else 1f, celebration) else null,
            stars = if (starBox != null) weeks else 0,
            starBox = starBox,
            amberStars = true,
            moon = moon,
            seed = 11 + weeks
        )
    }

    /** The moment's hue as text: green does the work, amber is earned, ember is to notice, dusk is quiet. */
    fun accentText(scope: FaceScope = this): Int = when (scope.data.moment.accent) {
        MomentAccent.ZEN -> t.brand
        MomentAccent.AMBER -> t.amberText
        MomentAccent.EMBER -> t.emberText
        MomentAccent.DUSK -> t.text3
    }
}
