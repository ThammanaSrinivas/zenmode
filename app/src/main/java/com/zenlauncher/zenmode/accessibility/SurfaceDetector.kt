package com.zenlauncher.zenmode.accessibility

/**
 * Abstraction over a snapshot of the on-screen accessibility node tree. The real
 * implementation (in [com.zenlauncher.zenmode.ZenAccessibilityService]) walks the
 * tree once per event; tests supply a fake. Keeping detection behind this
 * interface is what makes the matching logic unit-testable without Robolectric.
 */
interface NodeQuery {
    /** True if a node with this fully-qualified view-id is on screen. */
    fun hasViewId(viewId: String): Boolean

    /** True if a node with this view-id is on screen AND selected. */
    fun isViewIdSelected(viewId: String): Boolean

    /** True if a *selected* node's text or content-description equals this (case-insensitive). */
    fun hasSelectedText(text: String): Boolean
}

/**
 * Decides whether the currently visible screen of a tracked app is a blockable
 * surface. App-agnostic — it just evaluates whichever [AppRule] the caller passes
 * in (YouTube, Instagram, Snapchat all share this one detector).
 *
 * A [SurfaceRule] is a conjunction of whichever constraint lists it fills in;
 * an empty rule never matches. Two rules may share an `id` (e.g. "shorts" via the
 * player view-id OR via the selected bottom-tab label) — [detect] returns the
 * first that matches and the caller keys the user's on/off choice by `id`.
 */
object SurfaceDetector {

    fun detect(appRule: AppRule?, query: NodeQuery): SurfaceRule? {
        if (appRule == null) return null
        return appRule.surfaces.firstOrNull { matches(it, query) }
    }

    fun matches(surface: SurfaceRule, query: NodeQuery): Boolean {
        if (surface.allViewId.isEmpty() &&
            surface.anyViewId.isEmpty() &&
            surface.selectedViewId.isEmpty() &&
            surface.selectedText.isEmpty()
        ) return false

        if (surface.allViewId.isNotEmpty() && !surface.allViewId.all { query.hasViewId(it) }) {
            return false
        }
        if (surface.anyViewId.isNotEmpty() && surface.anyViewId.none { query.hasViewId(it) }) {
            return false
        }
        if (surface.selectedViewId.isNotEmpty() && surface.selectedViewId.none { query.isViewIdSelected(it) }) {
            return false
        }
        if (surface.selectedText.isNotEmpty() && surface.selectedText.none { query.hasSelectedText(it) }) {
            return false
        }
        return true
    }
}
