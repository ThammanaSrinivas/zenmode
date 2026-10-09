package com.zenlauncher.zenmode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Every launchable app, A–Z, with labels and icons resolved — and kept between Home resumes.
 *
 * Enumerating launcher activities and calling loadLabel/loadIcon for each one is two binder
 * round-trips per installed app, and Home resumes every single time the user comes back to
 * it: dozens to hundreds of times a day, on a ~150-app phone. The installed set only changes
 * when a package does, so a PACKAGE_* broadcast is what invalidates this ([load] with
 * `refresh = true`), not onResume.
 *
 * Reordering around the pinned apps is the cheap half and still runs on every call, so
 * changing the home apps reorders the grid immediately without re-reading the catalog.
 *
 * Lives outside MainActivity because the Activity is at the repo's 1000-line ceiling, and
 * this is self-contained: give it a scope, a way to read the pins, and somewhere to publish.
 */
internal class HomeAppCatalog(
    private val context: Context,
    private val scope: CoroutineScope,
    private val pinnedKeys: () -> List<String>,
    private val onOrdered: (List<AppInfo>) -> Unit
) {

    private var catalog: List<AppInfo>? = null
    private var job: Job? = null
    private var receiverRegistered = false

    /**
     * The only thing that can change the installed-app catalog. Context-registered rather than
     * declared in the manifest because PACKAGE_* are implicit broadcasts, which manifest
     * receivers stopped being given in Android 8.
     */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // REPLACED fires on app updates too, where the icon or label may have changed.
            load(refresh = true)
        }
    }

    /** Starts watching for installed-app changes. Pair with [detach]. */
    fun attach() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        // EXPORTED to match the rest of the app's receivers. These are protected system
        // broadcasts, and the worst a spoofed one could do is cost an extra catalog reload.
        ContextCompat.registerReceiver(context, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
    }

    fun detach() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(packageReceiver) }
        receiverRegistered = false
        job?.cancel()
    }

    /**
     * Publishes the home-ordered app list through `onOrdered`, loading the catalog first if
     * it isn't in hand. [refresh] drops what's cached — the installed set has changed.
     */
    fun load(refresh: Boolean = false) {
        if (refresh) catalog = null
        // Pins change often and reordering is cheap; the catalog behind it is what's costly.
        catalog?.let { cached ->
            onOrdered(LauncherActivities.orderForHome(cached, pinnedKeys()))
            return
        }
        if (job?.isActive == true) return
        job = scope.launch {
            val loaded = withContext(Dispatchers.IO) { query() }
            catalog = loaded
            onOrdered(LauncherActivities.orderForHome(loaded, pinnedKeys()))
        }
    }

    private fun query(): List<AppInfo> {
        val pm = context.packageManager
        val activities = LauncherActivities.query(pm)

        // ZenMode declares CATEGORY_LAUNCHER (so it's selectable as default home) alongside
        // CATEGORY_HOME, so it shows up in its own launcher query. Left in, it lists itself in
        // the drawer and in search; tapping it re-delivers an intent to the already-running
        // singleTask Activity via onNewIntent, which resets showSearch/etc — the search overlay
        // just vanishes, looking like the app crashed. Same exclusion
        // OnboardingViewModel.queryLaunchableApps applies.
        return activities
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(pm),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(pm),
                    activityClassName = resolveInfo.activityInfo.name,
                    key = LauncherActivities.selectionKey(resolveInfo, activities)
                )
            }
            .sortedBy { it.label.toString() }
    }
}
