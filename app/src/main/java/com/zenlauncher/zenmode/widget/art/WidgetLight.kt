package com.zenlauncher.zenmode.widget.art

import com.zenlauncher.zenmode.coreapi.week.DayPart
import kotlin.math.hypot

/**
 * The light every widget sits in: albeit's lit surface, the same ground the iPhone widgets use.
 * Nothing in it is a picture of something; it is light, and each part of it says one thing:
 *
 *  · the **daylight** is the time of day: a clean green light in the morning, a clear one in the
 *    afternoon, low and warm in the evening, and the night stage after 21:30;
 *  · the **wash** rising from the bottom is today: albeit's happy wash while the day is calm,
 *    the neutral one when it drifts under 6.0, the annoyed one once it is over the promise;
 *  · the **glow** is the moment's hue behind the hero (Zeno, the score, the coin): amber when
 *    the week is won, ember when it is on the line, dusk when it got away, green otherwise;
 *  · **stars** on a dark sky are the streak, one soft point a week; a crescent **moon** is a
 *    Quiet session.
 *
 * It never moves and never carries the only copy of a fact. This file is the pure part (what
 * goes where, in dp), so it is unit-tested; [SurfacePainter] lays it down.
 */
object WidgetLight {

    /** Today, as the wash shows it. */
    enum class Mood { CALM, DRIFT, OVER, NONE }

    /** The moment's hue, as a glow. */
    enum class Hue { ZEN, AMBER, EMBER, DUSK }

    /** A soft light centred at ([x], [y]) reaching [radius], all in dp. */
    data class Glow(val hue: Hue, val x: Float, val y: Float, val radius: Float, val celebration: Boolean = false)

    data class Moon(val x: Float, val y: Float, val r: Float)

    data class Box(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

    /** One soft point of light. */
    data class Star(val x: Float, val y: Float, val r: Float, val bright: Boolean)

    data class Spec(
        val w: Float,
        val h: Float,
        val mood: Mood = Mood.NONE,
        val part: DayPart = DayPart.AFTERNOON,
        val glow: Glow? = null,
        /** Stars placed by [seed] inside [starBox]; drawn only on a dark sky. */
        val stars: Int = 0,
        val starBox: Box? = null,
        /** Stars in the streak's amber rather than the night's pale. */
        val amberStars: Boolean = false,
        val moon: Moon? = null,
        val seed: Int = 7
    )

    /** Streak stars stop here: a sky, not a tally. */
    const val MAX_STARS = 30

    /** How far apart two stars sit at least, in dp. */
    private const val STAR_GAP = 11f

    /**
     * Where the stars go: seeded, so the same sky every time for the same streak; never closer
     * than [STAR_GAP] to each other and never on the moon. One in three is a fainter, smaller one.
     */
    fun stars(s: Spec): List<Star> {
        val count = s.stars.coerceAtMost(MAX_STARS)
        if (count <= 0) return emptyList()
        val box = s.starBox ?: Box(0f, 0f, s.w, s.h * 0.5f)
        val rnd = Lcg(s.seed)
        val placed = mutableListOf<Star>()
        var tries = 0
        while (placed.size < count && tries < count * 60) {
            tries++
            val x = box.x0 + rnd.next() * (box.x1 - box.x0)
            val y = box.y0 + rnd.next() * (box.y1 - box.y0)
            if (x < 3f || y < 3f || x > s.w - 3f || y > s.h - 3f) continue
            if (s.moon != null && hypot(x - s.moon.x, y - s.moon.y) < s.moon.r * 2.4f) continue
            if (placed.any { hypot(it.x - x, it.y - y) < STAR_GAP }) continue
            val bright = placed.size % 3 != 2
            placed += Star(x, y, if (bright) 1.15f else 0.75f, bright)
        }
        return placed
    }

    /** The design's Lehmer generator, so a seed always lights the same sky. */
    private class Lcg(seed: Int) {
        private var s: Long = (seed.toLong() * 2654435761L % MOD).let { if (it == 0L) 1L else it }
        fun next(): Float {
            s = s * 48271L % MOD
            return (s.toDouble() / MOD).toFloat()
        }

        private companion object {
            const val MOD = 2147483647L
        }
    }
}
