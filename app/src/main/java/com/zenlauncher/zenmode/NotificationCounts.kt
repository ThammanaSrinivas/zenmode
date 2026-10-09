package com.zenlauncher.zenmode

/**
 * Pure counting/diffing behind [ZenNotificationListenerService], split out so it can be
 * unit-tested without a live listener binding.
 *
 * The diff exists for battery reasons: the listener is woken for every notification posted
 * or removed on the device, screen off included, and the old code cleared and refilled the
 * observable map on every one of those. Each refill invalidated every home tile's badge even
 * when no count had actually changed. [applyTo] writes only what differs, so an unchanged
 * rebuild costs nothing downstream.
 */
internal object NotificationCounts {

    /** The only two fields of a StatusBarNotification the count depends on. */
    data class Entry(val packageName: String, val isOngoing: Boolean)

    /**
     * Badge counts per package. Ongoing notifications (music players, VPNs, downloads) are
     * skipped — they're persistent status, not something waiting to be read — and so are our
     * own, which would otherwise badge ZenMode's own tile.
     */
    fun countsOf(entries: List<Entry>, selfPackage: String): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (entry in entries) {
            if (entry.isOngoing || entry.packageName == selfPackage) continue
            counts[entry.packageName] = (counts[entry.packageName] ?: 0) + 1
        }
        return counts
    }

    /**
     * Writes [next] into [target] with the fewest possible mutations, leaving entries that
     * already hold the right value untouched. Returns true when something actually changed.
     */
    fun applyTo(target: MutableMap<String, Int>, next: Map<String, Int>): Boolean {
        var changed = false

        val gone = target.keys.filter { it !in next }
        for (key in gone) {
            target.remove(key)
            changed = true
        }
        for ((key, count) in next) {
            if (target[key] != count) {
                target[key] = count
                changed = true
            }
        }
        return changed
    }
}
