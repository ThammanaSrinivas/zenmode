/*
 * ZenMode - A local screen-time blocker and digital wellness app.
 * Copyright (C) 2026 Thammana Srinivas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zenlauncher.zenmode

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import kotlinx.coroutines.launch

// The Zen Bro / Zen Circle "buddy area" entry points and flow-control logic, split out of
// MainActivity.kt to keep it under the 1000-line ceiling. These are extension functions
// rather than class members because Kotlin has no partial classes -- the fields they touch
// on MainActivity are `internal` rather than `private` for that reason.

/**
 * Share-link/Use-a-code tap in circle mode: shares the existing circle's link/code
 * immediately, or creates one first and defers the actual share/copy to the
 * justEnteredCircle LaunchedEffect once that finishes. [fallback] runs unchanged
 * outside circle mode (the classic Buddy share/copy actions).
 */
internal fun MainActivity.startOrShareCircle(isCircleMode: Boolean, action: PendingCircleAction, fallback: () -> Unit) {
    if (!isCircleMode) {
        fallback()
        return
    }
    val existing = circleViewModel.uiState.value?.circle
    if (existing != null) {
        when (action) {
            PendingCircleAction.SHARE -> buddyConnector.shareCircleInvite(existing.id)
            PendingCircleAction.COPY -> buddyConnector.copyUserCode(existing.id, showToast = true)
        }
    } else {
        pendingCircleAutoAction = action
        circleViewModel.createCircle("My Zen Circle")
    }
    showZenCircle = true
}

/**
 * Single entry point for "open the buddy area" - Settings, an old push-notification
 * deep link, the home invite button and the home buddy card all funnel through here so
 * they all respect the cached [BuddyFlowPreferences] decision instead of hardcoding a
 * screen. See BuddyFlowPreferences for the decision rules.
 */
internal fun MainActivity.openBuddyFlow() {
    when (BuddyFlowPreferences.decision(this)) {
        BuddyFlow.ZEN_CIRCLE -> {
            // A real circle has no classic buddy relationship to check -- getBuddyUid()
            // alone missed circle-only users entirely, always bouncing them back to the
            // Connect screen instead of their actual dashboard.
            val hasSomethingToShow = repository.getBuddyUid() != null || !repository.getCachedCircleId().isNullOrEmpty()
            if (hasSomethingToShow) openZenCircleFromHome() else showBuddyConnect = true
        }
        BuddyFlow.ZEN_BUDDY_CLASSIC -> showBuddyBattle = true
        null -> {
            if (repository.getBuddyUid() != null) {
                showBuddyFlowMigrationPrompt = true
            } else {
                // Nothing to migrate - silently and permanently on Zen Circle.
                BuddyFlowPreferences.setDecision(this, BuddyFlow.ZEN_CIRCLE)
                showBuddyConnect = true
            }
        }
    }
}

/** Home's buddy card: straight to the circle dashboard, fetching the buddy's name alongside. */
internal fun MainActivity.openZenCircleFromHome() {
    connectSuccessJob?.cancel()
    connectSuccessJob = null
    connectedBuddyName = null
    circleBuddyName = null
    showZenCircle = true
    showBuddyConnect = true
    val buddyUid = repository.getBuddyUid() ?: return
    lifecycleScope.launch {
        circleBuddyName = runCatching { ServiceLocator.firestoreDataSource.getUser(buddyUid)?.displayName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}

/**
 * "Remove buddy" and "Leave Circle" — with one buddy per circle today, both end the
 * relationship. The result lands in the DisconnectResult effect in MainActivity's setContent.
 */
internal fun MainActivity.removeBuddy() {
    if (removingBuddy) return
    // Same preconditions disconnectBuddy() checks; without them it returns silently and
    // the confirm button would sit on "Removing…" forever.
    val signedIn = (repository.getUserUid() ?: ServiceLocator.authProvider.getCurrentUserId()) != null
    if (!signedIn || repository.getBuddyUid() == null) {
        Toast.makeText(this, "You don’t have a Zen Bro to remove.", Toast.LENGTH_SHORT).show()
        return
    }
    removingBuddy = true
    accountabilityViewModel.disconnectBuddy()
}

/** Auto-connect path for a zenmodeos.com/b/{code} App Link tap - same outcome as pasting
 *  the code into "Use a code", minus the inline status text (there's no field to show it in). */
internal fun MainActivity.connectWithInviteCode(targetUid: String) {
    lifecycleScope.launch {
        when (val result = buddyConnector.addBuddy(targetUid)) {
            is BuddyAddResult.Success -> {
                connectSuccessJob = lifecycleScope.launch {
                    kotlinx.coroutines.delay(700)
                    connectedBuddyName = result.buddyName
                }
            }
            is BuddyAddResult.AlreadyBuddies ->
                Toast.makeText(this@connectWithInviteCode, "You're already connected with ${result.buddyName}.", Toast.LENGTH_SHORT).show()
            is BuddyAddResult.SelfAdd ->
                Toast.makeText(this@connectWithInviteCode, "That's your own invite link.", Toast.LENGTH_SHORT).show()
            is BuddyAddResult.Error ->
                Toast.makeText(this@connectWithInviteCode, result.message, Toast.LENGTH_SHORT).show()
        }
    }
}

/** Closes the Zen Bro connect flow and resets its transient state. */
internal fun MainActivity.closeBuddyConnect() {
    connectSuccessJob?.cancel()
    connectSuccessJob = null
    showBuddyConnect = false
    connectedBuddyName = null
    showZenCircle = false
    circleBuddyName = null
    // Drops a still-pending invite-link code if the flow closed before it ever
    // reached the Connect screen (e.g. the migration prompt intercepted it).
    pendingInviteCode = null
}
