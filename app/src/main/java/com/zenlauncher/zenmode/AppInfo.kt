package com.zenlauncher.zenmode

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: CharSequence,
    val packageName: CharSequence,
    val icon: Drawable,
    val notificationCount: Int = 0,
    // Some OEM ROMs (e.g. MIUI) expose Phone and Contacts as two separate launcher
    // activities inside the same package (com.android.contacts). Deduping/launching
    // by packageName alone collapses them into one icon, so the exact launcher
    // activity is carried alongside the package name.
    val activityClassName: String = ""
)
