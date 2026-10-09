package com.zenlauncher.zenmode

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {

    @Test
    fun `an update from a build that never saw notes shows them`() {
        assertTrue(WhatsNew.shouldShow(seenVersionCode = null, isFreshInstall = false, notesVersionCode = 16))
    }

    @Test
    fun `a fresh install never sees the notes it installed with`() {
        assertFalse(WhatsNew.shouldShow(seenVersionCode = null, isFreshInstall = true, notesVersionCode = 16))
    }

    @Test
    fun `once seen they never come back`() {
        assertFalse(WhatsNew.shouldShow(seenVersionCode = 16, isFreshInstall = false, notesVersionCode = 16))
    }

    @Test
    fun `newer notes show once more, even to an install that started fresh`() {
        assertTrue(WhatsNew.shouldShow(seenVersionCode = 16, isFreshInstall = true, notesVersionCode = 17))
    }
}
