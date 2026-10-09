package com.zenlauncher.zenmode

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.widget.Toast

/**
 * Every launcher entry point on the device, the one source for the home grid and the home-app
 * pickers. Deduped by the actual activity (package + class), not package alone: some OEM ROMs
 * (e.g. MIUI) ship Phone and Contacts as two launcher activities in the same package, and
 * deduping by package silently drops one of the two icons.
 */
object LauncherActivities {

    fun query(pm: PackageManager): List<ResolveInfo> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .distinctBy { key(it) }

    /** Stable per-activity id, e.g. for list keys; two entries can share a package. */
    fun key(info: ResolveInfo): String = "${info.activityInfo.packageName}/${info.activityInfo.name}"

    /**
     * Identity to use for *selection/pinning* (home-app picks, pinned apps): bare packageName,
     * matching every value already persisted for the common case of one launcher activity per
     * package. Only widened to [key] (package+activity) for a package that ships more than one
     * launcher activity — e.g. MIUI shipping Phone and Contacts from the same package — so those
     * two stop collapsing onto one identity and get selected/deselected independently.
     */
    fun selectionKey(info: ResolveInfo, all: List<ResolveInfo>): String {
        val pkg = info.activityInfo.packageName
        val sharesPackage = all.count { it.activityInfo.packageName == pkg } > 1
        return if (sharesPackage) key(info) else pkg
    }

    /**
     * Home-grid order: the picked home apps lead in the order chosen, everything else stays
     * A–Z behind them. [apps] is expected already sorted by label.
     *
     * Split out from the package-manager query so changing the pins — which happens often —
     * doesn't re-enumerate every launcher activity and reload every icon. Pure, so the
     * ordering is unit-testable.
     */
    fun orderForHome(apps: List<AppInfo>, pinnedKeys: List<String>): List<AppInfo> {
        if (pinnedKeys.isEmpty()) return apps
        val rank = pinnedKeys.withIndex().associate { (i, key) -> key to i }
        val (pinned, rest) = apps.partition { it.key in rank }
        return pinned.sortedBy { rank.getValue(it.key) } + rest
    }
}

/**
 * Puts an outside app opened from home into its own task, the way the system launcher does.
 *
 * Home (MainActivity) is `singleTask` + `excludeFromRecents`, so without NEW_TASK the app was
 * stacked inside ZenMode's own task: it never showed up in Recents, and apps whose launch screen
 * closes itself when it isn't the root of its task (LinkedIn) didn't open at all.
 */
internal fun Intent.inOwnTask(): Intent = addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * Opens the app a home tile stands for, or refuses it when the Distraction Blocker has it
 * quieted. Lives here rather than inline in MainActivity's HomeScreen call so the launch
 * rules sit with the rest of the launcher-intent handling.
 *
 * Launches the exact activity the icon represents rather than
 * `packageManager.getLaunchIntentForPackage()`, which resolves a single "default" activity
 * per package and so can't tell Phone from Contacts when an OEM ships both from the same
 * package (e.g. MIUI's com.android.contacts).
 */
internal fun Activity.launchHomeApp(appInfo: AppInfo) {
    val packageName = appInfo.packageName.toString()
    if (ContentBlockPrefs.shouldQuietApp(this, packageName)) {
        Toast.makeText(
            this,
            "${appInfo.label} is quieted. Let it back in Settings → Distraction Blocker.",
            Toast.LENGTH_SHORT
        ).show()
        return
    }
    val launchIntent = if (appInfo.activityClassName.isNotEmpty()) {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, appInfo.activityClassName)
        }
    } else {
        packageManager.getLaunchIntentForPackage(packageName)
    }
    // RESET_TASK_IF_NEEDED: bring an app that's already running back as it was left, like the
    // system launcher does, instead of stacking a second copy of its launch screen.
    launchIntent?.inOwnTask()?.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)?.let(::startActivity)
}
