package com.zenlauncher.zenmode.share

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Streak cards, part two: the sky ───────────────────────────────
// From 30 days the streak is told in astronomy, because that's the scale it has reached: a full
// moon (one lunar cycle), moon and tide (two), a season over the hills (a quarter of a year),
// then the earth's own orbit — half drawn at 180 days, closed at 365, more rings for more years.

private const val TAU = (2 * PI).toFloat()

/** Moon phase icon: [phase] 0 = new, 0.5 = full, back to new at 1. Lit side on the right while waxing. */
internal fun drawMoonPhase(canvas: Canvas, path: Path, cx: Float, cy: Float, r: Float, phase: Float, lit: Paint, dark: Paint) {
    canvas.drawCircle(cx, cy, r, dark)
    val waxing = phase <= 0.5f
    val k = cos(phase * TAU)
    val rx = r * abs(k)
    path.reset()
    path.addArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 180f)
    path.arcTo(RectF(cx - rx, cy - r, cx + rx, cy + r), 90f, if (k > 0f) -180f else 180f)
    path.close()
    if (waxing) {
        canvas.drawPath(path, lit)
    } else {
        canvas.save()
        canvas.scale(-1f, 1f, cx, cy)
        canvas.drawPath(path, lit)
        canvas.restore()
    }
}

/** A seeded set of craters for a moon of radius 1. */
private fun craters(seed: Int) = Random(seed).let { rnd ->
    List(10) { floatArrayOf((rnd.nextFloat() - 0.5f) * 1.5f, (rnd.nextFloat() - 0.5f) * 1.5f, 0.05f + rnd.nextFloat() * 0.13f) }
}

/** A moon disc at ([cx], [cy]): lit from the upper left, faint craters, its own halo. */
private fun StreakScene.moon(canvas: Canvas, cx: Float, cy: Float, r: Float, alpha: Float, pits: List<FloatArray>) {
    if (alpha <= 0f) return
    kit.glow(canvas, cx, cy, r * 2.5f, c.amberOn, 0.2f * alpha)
    kit.glow(canvas, cx, cy, r * 1.5f, c.cream, 0.14f * alpha)
    shade.shader = RadialGradient(
        cx - r * 0.28f, cy - r * 0.32f, r * 1.25f,
        intArrayOf(c.cream, c.amberOn, blend(c.amberOn, c.stone300, 0.4f)),
        floatArrayOf(0f, 0.6f, 1f),
        Shader.TileMode.CLAMP
    )
    shade.alpha = (alpha * 255).toInt()
    canvas.drawCircle(cx, cy, r, shade)
    shade.shader = null
    shade.alpha = 255
    canvas.save()
    canvas.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
    pits.forEach { (dx, dy, pr) ->
        fill.color = withAlpha(c.stone400, 0.08f * alpha)
        canvas.drawCircle(cx + dx * r, cy + dy * r, pr * r, fill)
        stroke.strokeWidth = r * 0.012f
        stroke.color = withAlpha(c.white, 0.25f * alpha)
        canvas.drawArc(RectF(cx + (dx - pr) * r, cy + (dy - pr) * r, cx + (dx + pr) * r, cy + (dy + pr) * r), 20f, 120f, false, stroke)
    }
    canvas.restore()
}

// ── Days 30–59: one full moon ─────────────────────────────────────

/** The full moon with the count on its face, ringed by the thirty phases it took to get there. */
internal class MoonScene(kit: ShareKit, c: StreakColors, share: StreakShare) : StreakScene(kit, c, share) {
    private val hero = HeroNumber(kit, share.days, 186f, c.inkSurface, "DAYS KEPT", withAlpha(c.inkSurface, 0.55f))
    private val stars = StarField(seed = 30, count = 130, area = RectF(0f, 0f, 1080f, 1000f))
    private val pits = craters(share.days)
    private val phasePath = Path()
    private val lit = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas, t: Float, art: RectF) {
        kit.wash(canvas, ShareFormat.POST, 0f to c.skyTop, 0.62f to c.night, 1f to c.ink)
        stars.draw(canvas, c.cream, ShareMotion.beat(t, 0, 1_300), t, twinkle = true)
        val cx = art.centerX()
        val cy = art.centerY()
        val rise = ShareMotion.beat(t, 100, 1_050)
        val my = cy + 90f * (1f - rise)
        moon(canvas, cx, my, 194f, rise, pits)

        // One lunar cycle, new moon at the top, full at the bottom, round to new again.
        for (k in 0 until 30) {
            val p = ShareMotion.pop(t, 520 + k * 24, 380)
            if (p <= 0f) continue
            val a = -PI / 2 + k * TAU / 30
            val x = cx + cos(a).toFloat() * 300f
            val y = cy + sin(a).toFloat() * 300f
            lit.color = withAlpha(c.cream, 0.92f)
            dark.color = withAlpha(c.white, 0.1f)
            drawMoonPhase(canvas, phasePath, x, y, 11.5f * p, k / 30f, lit, dark)
        }
        hero.draw(canvas, cx, my, t, 380)
    }
}

// ── Days 60–89: moon and tide ─────────────────────────────────────

/** The moon over a dark sea, its light laid across the water — two moons, one in the sky and
 * one in the tide. */
internal class TideScene(kit: ShareKit, c: StreakColors, share: StreakShare) : StreakScene(kit, c, share) {
    private val hero = HeroNumber(kit, share.days, 150f, c.inkSurface, "DAYS", withAlpha(c.inkSurface, 0.55f))
    private val pits = craters(share.days + 3)
    private val wave = Path()
    private var stars: StarField? = null
    private val glints = Random(64).let { rnd ->
        List(32) { List(1 + rnd.nextInt(3)) { floatArrayOf(rnd.nextFloat() * 2f - 1f, 0.2f + rnd.nextFloat() * 0.6f, rnd.nextFloat() * TAU) } }
    }

    override fun draw(canvas: Canvas, t: Float, art: RectF) {
        val horizon = art.centerY() + 120f
        val hf = horizon / ShareFormat.POST.height
        kit.wash(
            canvas, ShareFormat.POST,
            0f to c.skyTop,
            (hf - 0.002f) to blend(c.night, c.skyGlow, 0.35f),
            hf to c.inkGain,
            1f to c.ink
        )
        val sky = stars ?: StarField(seed = 60, count = 110, area = RectF(0f, 0f, 1080f, horizon - 30f)).also { stars = it }
        sky.draw(canvas, c.cream, ShareMotion.beat(t, 0, 1_300), t, twinkle = true)

        val cx = art.centerX()
        val rise = ShareMotion.beat(t, 120, 1_100)
        val my = horizon - 200f + 120f * (1f - rise)
        canvas.save()
        canvas.clipRect(0f, 0f, 1080f, horizon)
        moon(canvas, cx, my, 150f, rise, pits)
        hero.draw(canvas, cx, my, t, 400)
        canvas.restore()

        // The moonlight road: broken glints on the water, spreading and fading towards us.
        val road = ShareMotion.beat(t, 520, 900)
        glints.forEachIndexed { j, row ->
            val f = j / glints.size.toFloat()
            val y = horizon + 10f + j * 13f + f * f * 60f
            val spread = 26f + f * 190f
            row.forEach { (offset, length, phase) ->
                val shimmer = 0.5f + 0.5f * sin(t / 380f + phase)
                val half = spread * length * 0.5f * (0.7f + 0.3f * shimmer)
                val x = cx + offset * spread * 0.75f + sin(t / 300f + j * 0.8f) * 6f * f
                fill.color = withAlpha(c.cream, 0.6f * (1f - f) * (0.55f + 0.45f * shimmer) * road)
                canvas.drawRoundRect(x - half, y, x + half, y + 3f + f * 3f, 3f, 3f, fill)
            }
        }
        // Swell lines across the sea.
        stroke.strokeWidth = 2f
        for (i in 0 until 6) {
            val y = horizon + 34f + i * (28f + i * 7f)
            wave.reset()
            var x = 0f
            wave.moveTo(x, y)
            while (x <= 1080f) {
                x += 18f
                wave.lineTo(x, y + sin(x / 150f * TAU - t / 1_400f * (i + 1)) * (2.5f + i * 0.9f))
            }
            stroke.color = withAlpha(c.white, 0.07f * ShareMotion.beat(t, 300 + i * 80, 600))
            canvas.drawPath(wave, stroke)
        }
        stroke.color = withAlpha(c.white, 0.16f * rise)
        canvas.drawLine(0f, horizon, 1080f, horizon, stroke)
    }
}

// ── Days 90–179: a whole season ───────────────────────────────────

/** Dawn over three ranges of hills, the count written on the rising sun. */
internal class SeasonScene(kit: ShareKit, c: StreakColors, share: StreakShare) : StreakScene(kit, c, share) {
    private val hero = HeroNumber(kit, share.days, 150f, c.inkSurface, "DAYS", withAlpha(c.inkSurface, 0.6f))
    private val hill = Path()
    private val ray = Path()

    override fun draw(canvas: Canvas, t: Float, art: RectF) {
        val cx = art.centerX()
        val sunY = art.centerY() - 20f
        kit.wash(
            canvas, ShareFormat.POST,
            0f to c.skyTop,
            0.22f to c.skyMid,
            (sunY + 200f) / ShareFormat.POST.height to blend(c.skyGlow, c.amber, 0.42f),
            1f to c.rankNear
        )
        val rise = ShareMotion.beat(t, 80, 1_200)
        val y = sunY + 170f * (1f - rise)
        kit.glow(canvas, cx, y, 680f, c.amber, 0.3f * rise)
        kit.glow(canvas, cx, y, 420f, c.ember, 0.14f * rise)

        // Rays, slowly turning.
        val open = ShareMotion.beat(t, 500, 1_000)
        shade.shader = RadialGradient(cx, y, 700f, intArrayOf(withAlpha(c.cream, 0.13f * open), withAlpha(c.cream, 0f)), null, Shader.TileMode.CLAMP)
        for (i in 0 until 12) {
            val a = i * TAU / 12 + t / 9_000f
            val spread = 0.05f
            ray.reset()
            ray.moveTo(cx, y)
            ray.lineTo(cx + cos(a - spread) * 900f, y + sin(a - spread) * 900f)
            ray.lineTo(cx + cos(a + spread) * 900f, y + sin(a + spread) * 900f)
            ray.close()
            canvas.drawPath(ray, shade)
        }
        shade.shader = null

        val r = 176f
        shade.shader = RadialGradient(cx, y - r * 0.25f, r * 1.2f, intArrayOf(c.cream, c.amber, c.ember), floatArrayOf(0f, 0.58f, 1f), Shader.TileMode.CLAMP)
        shade.alpha = (rise * 255).toInt()
        canvas.drawCircle(cx, y, r, shade)
        shade.shader = null
        shade.alpha = 255
        hero.draw(canvas, cx, y, t, 420)

        // Three ranges, far to near: each darker, later and further down.
        val ground = art.bottom
        val ranges = listOf(
            Triple(ground - 170f, blend(c.skyGlow, c.ridge, 0.5f), 0f),
            Triple(ground - 105f, c.rankMid, 1.7f),
            Triple(ground - 40f, c.rankNear, 3.1f)
        )
        ranges.forEachIndexed { i, (top, color, seed) ->
            val p = ShareMotion.beat(t, 200 + i * 130, 900)
            val lift = 120f * (1f - p)
            hill.reset()
            hill.moveTo(0f, 1350f)
            var x = 0f
            while (x <= 1090f) {
                val h = 26f * sin(x / 1080f * TAU * (0.8f + i * 0.35f) + seed) + 14f * sin(x / 1080f * TAU * 2.3f + seed * 2f)
                hill.lineTo(x, top + h + lift)
                x += 15f
            }
            hill.lineTo(1080f, 1350f)
            hill.close()
            fill.color = color
            canvas.drawPath(hill, fill)
            if (i == 0) kit.glow(canvas, cx, top + 30f, 560f, c.mist, 0.06f * p)
        }

        // Three birds crossing the dawn.
        val birds = ShareMotion.beat(t, 1_000, 700)
        stroke.strokeWidth = 3.2f
        stroke.color = withAlpha(c.inkSurface, 0.6f * birds)
        for (i in 0 until 3) {
            val bx = cx + 230f + i * 58f + (t / 90f) % 40f
            val by = y - 170f - i * 34f + sin(t / 400f + i) * 4f
            val flap = 9f + 4f * sin(t / 160f + i * 2f)
            canvas.drawLine(bx - 14f, by - flap * 0.6f, bx, by, stroke)
            canvas.drawLine(bx, by, bx + 14f, by - flap * 0.6f, stroke)
        }
    }
}

// ── Days 180+: the orbit ──────────────────────────────────────────

/**
 * The sun with the count on it and the earth's orbit round it, tilted so the near half passes in
 * front of the sun and the far half behind. The streak is the lit stretch of the orbit: half of
 * it at 180 days, all of it at 365 — then a laurel of 365 ticks and a ring for every extra year.
 */
internal class OrbitScene(kit: ShareKit, c: StreakColors, share: StreakShare) : StreakScene(kit, c, share) {
    private val full = share.tier == StreakTier.ORBIT
    private val reach = if (full) 1f else (share.days / 365f).coerceIn(0f, 1f)
    private val sunR = if (full) 166f else 150f
    private val hero = HeroNumber(kit, share.days, 150f, c.inkSurface, "DAYS", withAlpha(c.inkSurface, 0.6f))
    private val stars = StarField(seed = if (full) 365 else 180, count = 160, area = RectF(0f, 0f, 1080f, 1350f), maxR = 3.4f)
    private val back = Path()
    private val front = Path()
    private val glints = Random(share.days).let { rnd -> List(5) { rnd.nextFloat() * TAU } }

    private val rx = 400f
    private val ry = 150f
    private val tilt = -12f

    override fun draw(canvas: Canvas, t: Float, art: RectF) {
        kit.wash(canvas, ShareFormat.POST, 0f to c.night, 0.55f to c.ink, 1f to c.ink)
        stars.draw(canvas, c.cream, ShareMotion.beat(t, 0, 1_200), t, twinkle = true)
        val cx = art.centerX()
        val cy = art.centerY() + 10f
        kit.glow(canvas, cx, cy, 760f, c.skyGlow, 0.16f)

        val sweep = ShareMotion.beat(t, 380, 1_300) * reach
        val years = share.orbits.coerceIn(0, 3)

        // Far half first, so the sun covers it.
        canvas.save()
        canvas.rotate(tilt, cx, cy)
        for (y in 1 until years) ring(canvas, cx, cy, rx + 46f * y, ry + 18f * y, farSide = true, alpha = 0.5f / y)
        orbit(canvas, cx, cy, sweep, farSide = true, t = t)
        canvas.restore()

        val s = ShareMotion.pop(t, 120, 760, overshoot = 1.1f)
        kit.glow(canvas, cx, cy, 480f, c.amber, 0.28f * s)
        kit.glow(canvas, cx, cy, 300f, c.ember, 0.12f * s)
        if (full) laurel(canvas, cx, cy, ShareMotion.beat(t, 700, 1_000))
        if (s > 0f) {
            shade.shader = RadialGradient(cx, cy - sunR * 0.3f, sunR * 1.25f, intArrayOf(c.cream, c.amber, c.ember), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, sunR * s, shade)
            shade.shader = null
        }
        hero.draw(canvas, cx, cy, t, 420)

        canvas.save()
        canvas.rotate(tilt, cx, cy)
        for (y in 1 until years) ring(canvas, cx, cy, rx + 46f * y, ry + 18f * y, farSide = false, alpha = 0.5f / y)
        orbit(canvas, cx, cy, sweep, farSide = false, t = t)
        canvas.restore()
    }

    /** One side of the orbit: the dim track, the lit stretch of streak, month dots and the earth. */
    private fun orbit(canvas: Canvas, cx: Float, cy: Float, sweep: Float, farSide: Boolean, t: Float) {
        ring(canvas, cx, cy, rx, ry, farSide, alpha = 1f)

        // The streak starts at the near middle and runs anticlockwise round to today.
        val start = PI.toFloat() / 2f
        val end = start + sweep * TAU
        back.reset()
        front.reset()
        // Segment by segment, each onto the half its midpoint is on; a half picks up again with a
        // moveTo when the orbit comes back round to it, and the two meet without a gap.
        var lastBack = -1
        var lastFront = -1
        var i = 0
        var a = start
        var px = cx + cos(a) * rx
        var py = cy + sin(a) * ry
        while (a < end) {
            val next = minOf(a + STEP, end)
            val nx = cx + cos(next) * rx
            val ny = cy + sin(next) * ry
            val isFar = sin((a + next) / 2f) < 0f
            val path = if (isFar) back else front
            if ((if (isFar) lastBack else lastFront) != i) path.moveTo(px, py)
            path.lineTo(nx, ny)
            if (isFar) lastBack = i + 1 else lastFront = i + 1
            px = nx
            py = ny
            a = next
            i++
        }
        val lit = if (farSide) back else front
        stroke.strokeWidth = 18f
        stroke.color = withAlpha(c.amber, if (farSide) 0.1f else 0.18f)
        canvas.drawPath(lit, stroke)
        stroke.strokeWidth = 5f
        stroke.color = withAlpha(c.amber, if (farSide) 0.75f else 1f)
        canvas.drawPath(lit, stroke)

        for (m in 0 until 12) {
            val ma = start + m * TAU / 12
            if ((sin(ma) < 0f) != farSide) continue
            fill.color = withAlpha(c.white, 0.42f)
            canvas.drawCircle(cx + cos(ma) * rx, cy + sin(ma) * ry, 3.6f, fill)
        }
        if (full) {
            glints.forEach { g ->
                if ((sin(g) < 0f) != farSide) return@forEach
                val tw = 0.5f + 0.5f * sin(t / 300f + g * 5f)
                sparkle(canvas, cx + cos(g) * rx, cy + sin(g) * ry, 16f * tw, withAlpha(c.cream, 0.9f * tw))
            }
        }

        if (sweep > 0f && (sin(end) < 0f) == farSide) {
            val ex = cx + cos(end) * rx
            val ey = cy + sin(end) * ry
            kit.glow(canvas, ex, ey, 58f, c.zen300, 0.4f)
            fill.color = c.zen300
            canvas.drawCircle(ex, ey, 15f, fill)
            fill.color = withAlpha(c.white, 0.8f)
            canvas.drawCircle(ex - 4.5f, ey - 4.5f, 5f, fill)
        }
    }

    private fun ring(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, farSide: Boolean, alpha: Float) {
        stroke.strokeWidth = 2.5f
        stroke.color = withAlpha(c.white, (if (farSide) 0.12f else 0.22f) * alpha)
        canvas.drawArc(RectF(cx - rx, cy - ry, cx + rx, cy + ry), if (farSide) 180f else 0f, 180f, false, stroke)
    }

    /** 365 hairline ticks round the sun, one per day of the year, drawn in from the top. */
    private fun laurel(canvas: Canvas, cx: Float, cy: Float, p: Float) {
        if (p <= 0f) return
        stroke.strokeWidth = 2f
        stroke.color = withAlpha(c.amber, 0.55f)
        val n = (365 * p).toInt()
        for (i in 0 until n) {
            val a = -PI.toFloat() / 2f + i * TAU / 365
            val inner = sunR + 26f
            val outer = sunR + if (i % 30 == 0) 52f else 40f
            canvas.drawLine(cx + cos(a) * inner, cy + sin(a) * inner, cx + cos(a) * outer, cy + sin(a) * outer, stroke)
        }
    }

    private fun sparkle(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
        if (r <= 0.5f) return
        stroke.strokeWidth = 2.4f
        stroke.color = color
        canvas.drawLine(x - r, y, x + r, y, stroke)
        canvas.drawLine(x, y - r, x, y + r, stroke)
    }

    private companion object {
        const val STEP = 0.02f
    }
}
