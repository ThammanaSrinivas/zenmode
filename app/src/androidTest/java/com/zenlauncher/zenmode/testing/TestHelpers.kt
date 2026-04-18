package com.zenlauncher.zenmode.testing

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.AccountabilityUiState
import com.zenlauncher.zenmode.coreapi.DailyUsage

/**
 * Factory functions for test data used across instrumented tests.
 */
object TestData {

    /** 2 hours of screen time */
    val twoHoursUsage = DailyUsage(screenTimeInMillis = 2 * 60 * 60 * 1000L)

    /** 30 minutes of screen time */
    val thirtyMinUsage = DailyUsage(screenTimeInMillis = 30 * 60 * 1000L)

    /** 4 hours (high usage / annoyed mood) */
    val highUsage = DailyUsage(screenTimeInMillis = 4 * 60 * 60 * 1000L)

    /** Zero screen time */
    val zeroUsage = DailyUsage(screenTimeInMillis = 0L)

    val defaultBuddyStats = BuddyStats(screenTimeMins = 90)

    fun createAppInfo(
        label: String = "Test App",
        packageName: String = "com.test.app",
        isPinned: Boolean = false,
        notificationCount: Int = 0
    ) = AppInfo(
        label = label,
        packageName = packageName,
        icon = ColorDrawable(Color.BLUE),
        isPinned = isPinned,
        notificationCount = notificationCount
    )

    fun createAppList(count: Int = 5): List<AppInfo> =
        (1..count).map { i ->
            createAppInfo(
                label = "App $i",
                packageName = "com.test.app$i"
            )
        }

    fun createAccountabilityUiState(
        userCode: String? = "ABC123",
        buddyStats: BuddyStats? = null,
        connectionDateMillis: Long? = null
    ) = AccountabilityUiState(
        myUsage = twoHoursUsage,
        buddyStats = buddyStats,
        userCode = userCode,
        connectionDateMillis = connectionDateMillis
    )
}
