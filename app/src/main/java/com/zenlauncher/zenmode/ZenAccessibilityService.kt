package com.zenlauncher.zenmode

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class ZenAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_LOCK_SCREEN = "com.zenlauncher.zenmode.ACTION_LOCK_SCREEN"
        private var instance: ZenAccessibilityService? = null

        fun lockScreen(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
        }

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op — service is used for global actions only
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
