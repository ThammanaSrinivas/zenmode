package com.zenlauncher.zenmode.coreapi

/** One continuous stretch of an app in the foreground, in epoch millis. */
data class ForegroundSession(
    val packageName: String,
    val startMillis: Long,
    val endMillis: Long
)
