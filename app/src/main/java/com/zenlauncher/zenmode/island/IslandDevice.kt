package com.zenlauncher.zenmode.island

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlin.math.abs

/**
 * What this phone can carry, and where on its screen the island sits.
 *
 * Both halves are pure ([tier] and [IslandGeometry.of]) so the rules are unit-tested; the two
 * readers below only collect the facts from Android.
 */
object IslandDevice {

    /**
     * [FULL]: we are confident the phone carries the island smoothly, so it starts on.
     * [LITE]: a low-memory or older phone. The island starts off, the person can turn it on,
     * and it then runs with fewer effects (no glow, no moving bars).
     */
    enum class Tier { FULL, LITE }

    data class Facts(
        val sdk: Int,
        val lowRam: Boolean,
        val totalRamMb: Long,
        /** Android's media performance class (API 31+): 30 or more means a phone Google has graded. */
        val performanceClass: Int,
        val cores: Int
    )

    /**
     * Phones report a little under their marketed RAM, so the steps sit below 3, 4 and 6 GB.
     * Confident means a graded performance class, 6 GB, or 4 GB with eight cores on Android 12+.
     */
    fun tier(f: Facts): Tier = when {
        f.lowRam || f.sdk < 29 || f.totalRamMb < 2_800 -> Tier.LITE
        f.performanceClass >= 30 -> Tier.FULL
        f.totalRamMb >= 5_000 -> Tier.FULL
        f.totalRamMb >= 3_300 && f.sdk >= 31 && f.cores >= 8 -> Tier.FULL
        else -> Tier.LITE
    }

    fun facts(context: Context): Facts {
        val am = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also { runCatching { am?.getMemoryInfo(it) } }
        val graded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.VERSION.MEDIA_PERFORMANCE_CLASS else 0
        return Facts(
            sdk = Build.VERSION.SDK_INT,
            lowRam = am?.isLowRamDevice == true,
            totalRamMb = memory.totalMem / (1024L * 1024L),
            performanceClass = graded,
            cores = Runtime.getRuntime().availableProcessors()
        )
    }

    fun tier(context: Context): Tier = tier(facts(context))
}

/** Where the island is drawn: around the camera (above the status bar) or docked just under it. */
enum class Placement { CAMERA, DOCKED }

/** A display cutout's rectangle, in px. */
data class Cutout(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * The island's resting place on this screen, in px. The pill and the card share [centerX], so
 * the window can grow and shrink around it without the island moving.
 */
data class IslandGeometry(
    val screenWidth: Float,
    val centerX: Float,
    /** Top of the pill, and of the card that grows out of it. */
    val top: Float,
    val pillHeight: Float,
    /** Width kept clear in the middle of the pill for the camera (a small gap when there is none). */
    val cameraGap: Float,
    /** The pill wraps a camera hole. */
    val onCamera: Boolean,
    val density: Float,
    /** Room the open card's top row leaves at its start and end for a camera in a corner, in px. */
    val avoidStart: Float = 0f,
    val avoidEnd: Float = 0f
) {
    /** The open card's width: the screen less a margin, never wider than a phone. */
    val cardWidth: Float
        get() {
            val margin = CARD_MARGIN_DP * density + abs(centerX - screenWidth / 2f)
            return (screenWidth - 2 * margin).coerceAtMost(CARD_MAX_DP * density)
        }

    /**
     * A camera off to one side would sit over the card's top row (the card spans the screen):
     * the row starts after it, or ends before it.
     */
    private fun clearOf(cutouts: List<Cutout>): IslandGeometry {
        if (onCamera) return this
        val cardLeft = centerX - cardWidth / 2f
        val cardRight = centerX + cardWidth / 2f
        val bandBottom = top + pillHeight
        var start = 0f
        var end = 0f
        cutouts.filter { it.width > 0f && it.top < bandBottom && it.bottom > top }.forEach { c ->
            if (c.centerX < screenWidth / 2f) start = maxOf(start, c.right - cardLeft + 6 * density)
            else end = maxOf(end, cardRight - c.left + 6 * density)
        }
        return copy(avoidStart = start.coerceAtLeast(0f), avoidEnd = end.coerceAtLeast(0f))
    }

    companion object {
        const val CARD_MARGIN_DP = 9f
        const val CARD_MAX_DP = 412f

        /**
         * [statusBar] is the status bar's height. A camera counts as centred when its middle is
         * within 12% of the screen's and it is a hole or a small notch, not a wide one.
         */
        fun of(screenWidth: Float, statusBar: Float, cutouts: List<Cutout>, density: Float, placement: Placement): IslandGeometry =
            place(screenWidth, statusBar, cutouts, density, placement).clearOf(cutouts)

        private fun place(screenWidth: Float, statusBar: Float, cutouts: List<Cutout>, density: Float, placement: Placement): IslandGeometry {
            val dp = density
            if (placement == Placement.DOCKED) {
                val h = 32 * dp
                return IslandGeometry(screenWidth, screenWidth / 2f, statusBar + 6 * dp, h, 8 * dp, false, density)
            }
            val centred = cutouts.firstOrNull { c ->
                c.top < statusBar.coerceAtLeast(40 * dp) && c.bottom <= statusBar + 48 * dp &&
                    abs(c.centerX - screenWidth / 2f) <= screenWidth * 0.12f
            }
            return when {
                centred != null && centred.width <= screenWidth * 0.3f -> {
                    val h = (centred.height + 12 * dp).coerceIn(26 * dp, 38 * dp)
                    IslandGeometry(
                        screenWidth, centred.centerX, (centred.centerY - h / 2f).coerceAtLeast(1 * dp), h,
                        centred.width + 10 * dp, true, density
                    )
                }
                // A wide notch: sit just under it rather than hide words behind it.
                centred != null -> IslandGeometry(screenWidth, screenWidth / 2f, centred.bottom + 4 * dp, 30 * dp, 8 * dp, false, density)
                else -> {
                    val h = (statusBar - 6 * dp).coerceIn(24 * dp, 34 * dp)
                    val top = if (statusBar <= 0f) 6 * dp else ((statusBar - h) / 2f).coerceAtLeast(2 * dp)
                    IslandGeometry(screenWidth, screenWidth / 2f, top, h, 8 * dp, false, density)
                }
            }
        }
    }
}
