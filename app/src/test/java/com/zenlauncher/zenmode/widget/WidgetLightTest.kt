package com.zenlauncher.zenmode.widget

import com.zenlauncher.zenmode.widget.art.WidgetLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/** The light behind every widget: where its stars go is the only placement it makes. */
class WidgetLightTest {

    private val box = WidgetLight.Box(10f, 10f, 174f, 120f)

    @Test
    fun `one star a won week, stopping at thirty`() {
        assertEquals(9, WidgetLight.stars(WidgetLight.Spec(184f, 220f, stars = 9, starBox = box)).size)
        val sky = WidgetLight.Spec(380f, 456f, stars = 64, starBox = WidgetLight.Box(0f, 0f, 380f, 300f))
        assertEquals(WidgetLight.MAX_STARS, WidgetLight.stars(sky).size)
        assertTrue(WidgetLight.stars(WidgetLight.Spec(184f, 220f)).isEmpty())
    }

    @Test
    fun `stars keep their distance, stay in their box and off the moon`() {
        val moon = WidgetLight.Moon(150f, 40f, 12f)
        val stars = WidgetLight.stars(WidgetLight.Spec(184f, 220f, stars = 12, starBox = box, moon = moon))
        stars.forEach { s ->
            assertTrue("$s outside", s.x in box.x0..box.x1 && s.y in box.y0..box.y1)
            assertTrue("$s on the moon", hypot(s.x - moon.x, s.y - moon.y) >= moon.r * 2.4f)
        }
        stars.forEachIndexed { i, a -> stars.drop(i + 1).forEach { b -> assertTrue("$a $b", hypot(a.x - b.x, a.y - b.y) >= 11f) } }
    }

    @Test
    fun `the same streak lights the same sky`() {
        val spec = WidgetLight.Spec(184f, 220f, stars = 7, starBox = box, seed = 18)
        assertEquals(WidgetLight.stars(spec), WidgetLight.stars(spec))
    }

    @Test
    fun `one star in three is fainter`() {
        val stars = WidgetLight.stars(WidgetLight.Spec(184f, 220f, stars = 9, starBox = box))
        assertEquals(3, stars.count { !it.bright })
    }
}
