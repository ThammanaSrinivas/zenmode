package com.zenlauncher.zenmode.coreapi

data class DailyUsage(
    val screenTimeInMillis: Long,
    val usagePermissionGranted: Boolean = true
)
