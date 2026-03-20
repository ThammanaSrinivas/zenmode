package com.zenlauncher.zenmode

import android.graphics.drawable.Drawable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AppInfoTest {

    @Test
    fun `AppInfo holds correct data`() {
        val icon = mock<Drawable>()
        val appInfo = AppInfo("Label", "com.zenlauncher.zenmode", icon)
        
        assertEquals("Label", appInfo.label)
        assertEquals("com.zenlauncher.zenmode", appInfo.packageName)
        assertEquals(icon, appInfo.icon)
    }
}
