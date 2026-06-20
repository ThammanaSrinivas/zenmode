package com.zenlauncher.zenmode.coreapi

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

/**
 * Single source of truth for the "Usage Access" (PACKAGE_USAGE_STATS) grant check.
 *
 * Lives in core-api so [UsageRepository] can gate reads on it. The app module previously
 * duplicated this AppOps check in three places (MainActivity, UsageAccessPermissionScreen,
 * DoomScrollingMonitorService) — they now all delegate here.
 */
object UsageAccess {
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
