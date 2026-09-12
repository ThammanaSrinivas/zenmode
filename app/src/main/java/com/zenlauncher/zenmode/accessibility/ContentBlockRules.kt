package com.zenlauncher.zenmode.accessibility

import org.json.JSONArray
import org.json.JSONObject

/**
 * A single blockable in-app surface (e.g. "YouTube Shorts").
 *
 * Matching ([SurfaceDetector.matches]) is the conjunction of whichever of
 * these lists are non-empty:
 *  - [allViewId]      — every view-id must be present on screen
 *  - [anyViewId]      — at least one view-id must be present
 *  - [selectedViewId] — at least one of these view-ids must be present AND selected
 *  - [selectedText]   — some selected node's text/content-description must equal one of these
 *
 * An all-empty rule never matches. On a match the service presses Back.
 */
data class SurfaceRule(
    val id: String,
    val label: String,
    val allViewId: List<String> = emptyList(),
    val anyViewId: List<String> = emptyList(),
    val selectedViewId: List<String> = emptyList(),
    val selectedText: List<String> = emptyList()
)

data class AppRule(
    val packageName: String,
    val surfaces: List<SurfaceRule>
)

/**
 * The in-app content-blocking ruleset. Hardcoded default lives in [default];
 * [parse] exists so a future Remote Config override can swap the source without
 * touching the detection code (see docs/engineering/service-interactions.md).
 *
 * Scope today: short-form feeds only — YouTube Shorts, Instagram Reels, Snapchat
 * Spotlight (same targets as the FlowBit reference app). YouTube home-feed
 * blocking was tried and pulled — see docs/plans/2026-09-youtube-content-blocking.md.
 */
data class ContentBlockRules(
    val version: Int,
    val apps: Map<String, AppRule>
) {
    fun appRule(packageName: String): AppRule? = apps[packageName]

    companion object {
        const val YOUTUBE = "com.google.android.youtube"
        const val INSTAGRAM = "com.instagram.android"
        const val SNAPCHAT = "com.snapchat.android"

        /** Every package the service should be woken for — keep in sync with
         *  `accessibility_service_config.xml`'s `android:packageNames`. */
        val TRACKED_PACKAGES = listOf(YOUTUBE, INSTAGRAM, SNAPCHAT)

        val EMPTY = ContentBlockRules(0, emptyMap())

        /**
         * Bundled default — same short-form surfaces the FlowBit reference app blocks.
         * Each surface lists the primary view-id first, then fallbacks in case it is
         * renamed in a future app version.
         */
        fun default(): ContentBlockRules = ContentBlockRules(
            version = 4,
            apps = mapOf(
                YOUTUBE to AppRule(
                    packageName = YOUTUBE,
                    surfaces = listOf(
                        SurfaceRule(
                            id = "shorts",
                            label = "Shorts",
                            anyViewId = listOf(
                                "$YOUTUBE:id/reel_recycler",
                                "$YOUTUBE:id/reel_watch_fragment_root",
                                "$YOUTUBE:id/reel_player_overlay",
                                "$YOUTUBE:id/reel_progress_bar",
                                "$YOUTUBE:id/shorts_video_container"
                            )
                        )
                    )
                ),
                INSTAGRAM to AppRule(
                    packageName = INSTAGRAM,
                    surfaces = listOf(
                        SurfaceRule(
                            id = "reels",
                            label = "Reels",
                            anyViewId = listOf(
                                "$INSTAGRAM:id/clips_viewer_view_pager",
                                "$INSTAGRAM:id/clips_viewer_root",
                                "$INSTAGRAM:id/clips_video_container"
                            )
                        )
                    )
                ),
                SNAPCHAT to AppRule(
                    packageName = SNAPCHAT,
                    surfaces = listOf(
                        SurfaceRule(
                            id = "spotlight",
                            label = "Spotlight",
                            anyViewId = listOf(
                                "$SNAPCHAT:id/spotlight_container",
                                "$SNAPCHAT:id/spotlight_view_pager",
                                "$SNAPCHAT:id/spotlight_root"
                            )
                        )
                    )
                )
            )
        )

        fun parse(json: String): ContentBlockRules {
            return try {
                val root = JSONObject(json)
                val version = root.optInt("version", 0)
                val appsObj = root.optJSONObject("apps") ?: JSONObject()
                val apps = mutableMapOf<String, AppRule>()
                for (pkg in appsObj.keys()) {
                    val appObj = appsObj.getJSONObject(pkg)
                    val surfacesArr = appObj.optJSONArray("surfaces") ?: continue
                    val surfaces = mutableListOf<SurfaceRule>()
                    for (i in 0 until surfacesArr.length()) {
                        val s = surfacesArr.getJSONObject(i)
                        val id = s.optString("id")
                        if (id.isNullOrEmpty()) continue
                        surfaces.add(
                            SurfaceRule(
                                id = id,
                                label = s.optString("label", id),
                                allViewId = s.optJSONArray("allViewId").toStringList(),
                                anyViewId = s.optJSONArray("anyViewId").toStringList(),
                                selectedViewId = s.optJSONArray("selectedViewId").toStringList(),
                                selectedText = s.optJSONArray("selectedText").toStringList()
                            )
                        )
                    }
                    if (surfaces.isNotEmpty()) apps[pkg] = AppRule(pkg, surfaces)
                }
                ContentBlockRules(version, apps)
            } catch (_: Exception) {
                EMPTY
            }
        }

        private fun JSONArray?.toStringList(): List<String> {
            if (this == null) return emptyList()
            return (0 until length()).mapNotNull { idx ->
                optString(idx).takeIf { it.isNotEmpty() }
            }
        }
    }
}
