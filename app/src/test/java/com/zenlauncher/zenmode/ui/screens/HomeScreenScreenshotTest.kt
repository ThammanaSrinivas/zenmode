package com.zenlauncher.zenmode.ui.screens

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenlauncher.zenmode.AppInfo
import com.zenlauncher.zenmode.BuddyStats
import com.zenlauncher.zenmode.HomeStackMember
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import org.junit.Rule
import org.junit.Test

/**
 * Golden/snapshot coverage for HomeScreen, the one screen already migrated to
 * v3 tokens (CLAUDE.md). Two representative states rather than an exhaustive
 * matrix, to keep the baseline set small enough to actually review. See
 * app/config/legacy-color-debt.txt / checkSourceOfTruth for the companion
 * guardrail this backs up: a wrong hardcoded color here would previously
 * only be caught by a human eyeballing the app; now it fails the build.
 *
 * Baselines: `./gradlew recordPaparazzi` generates the PNGs under
 * app/src/test/snapshots/ - review them once before committing, they become
 * ground truth. `./gradlew verifyPaparazzi` (wired into `check`) compares
 * against them from then on.
 */
class HomeScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `home screen - happy mood, signed in with buddy`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                HomeScreen(
                    usage = DailyUsage(screenTimeInMillis = 5 * 60 * 1000L),
                    streaks = 3,
                    yesterdayChangePercent = -10,
                    hasBuddies = true,
                    buddyStats = BuddyStats(screenTimeMins = 20L),
                    isSignedIn = true,
                    showSearch = false,
                    zenScore = 80,
                    goldInvested = "0",
                    goldChangePercent = 5,
                    myLikes = 2L,
                    buddyLikes = 1L,
                    onShowSearchChange = {},
                    onGoogleSearch = {},
                    onPhoneClick = {},
                    onLockClick = {},
                    onInviteBuddyClick = {},
                    onSignInClick = {},
                    onAppClick = {},
                    apps = listOf(
                        AppInfo(
                            label = "Sample App",
                            packageName = "com.example.sample",
                            icon = ColorDrawable(AndroidColor.DKGRAY)
                        )
                    )
                )
            }
        }
    }

    @Test
    fun `home screen - annoyed mood, signed out no buddy`() {
        paparazzi.golden {
            ZenTheme(darkTheme = false) {
                HomeScreen(
                    usage = DailyUsage(screenTimeInMillis = 240 * 60 * 1000L),
                    streaks = 0,
                    yesterdayChangePercent = 15,
                    hasBuddies = false,
                    buddyStats = null,
                    isSignedIn = false,
                    showSearch = false,
                    zenScore = 20,
                    goldInvested = "0",
                    goldChangePercent = 0,
                    onShowSearchChange = {},
                    onGoogleSearch = {},
                    onPhoneClick = {},
                    onLockClick = {},
                    onInviteBuddyClick = {},
                    onSignInClick = {},
                    onAppClick = {},
                    apps = emptyList()
                )
            }
        }
    }

    @Test
    fun `home screen - zen circle stack, three members`() {
        paparazzi.golden {
            // Paparazzi leaves inspection mode off, same as ZenCircleScreenScreenshotTest; turn
            // it on so the header/cards/grid reveal animations render settled instead of stuck
            // at their pre-entrance state (the two tests above already snapshot into that gap --
            // not fixed here since it'd change their recorded baselines).
            CompositionLocalProvider(LocalInspectionMode provides true) {
            ZenTheme(darkTheme = false) {
                HomeScreen(
                    usage = DailyUsage(screenTimeInMillis = 5 * 60 * 1000L),
                    streaks = 3,
                    yesterdayChangePercent = -10,
                    hasBuddies = true,
                    buddyStats = BuddyStats(screenTimeMins = 20L),
                    isSignedIn = true,
                    showSearch = false,
                    zenScore = 80,
                    goldInvested = "0",
                    goldChangePercent = 5,
                    circleStackMembers = listOf(
                        HomeStackMember("uid-1", "SriniMas", BuddyStats(screenTimeMins = 20L), zenScore = 74, lastUpdatedEpochMs = 3L),
                        HomeStackMember("uid-2", "Kamal", BuddyStats(screenTimeMins = 55L), zenScore = 61, lastUpdatedEpochMs = 2L),
                        HomeStackMember("uid-3", "Alex", BuddyStats(screenTimeMins = 12L), zenScore = 89, lastUpdatedEpochMs = 1L)
                    ),
                    onShowSearchChange = {},
                    onGoogleSearch = {},
                    onPhoneClick = {},
                    onLockClick = {},
                    onInviteBuddyClick = {},
                    onSignInClick = {},
                    onAppClick = {},
                    apps = emptyList()
                )
            }
            }
        }
    }
}
