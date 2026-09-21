package com.zenlauncher.zenmode

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

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
}
