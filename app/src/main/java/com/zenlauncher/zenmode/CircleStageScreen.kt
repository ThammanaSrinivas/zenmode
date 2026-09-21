package com.zenlauncher.zenmode

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import com.zenlauncher.zenmode.coreapi.Circle
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.ReactionType
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.ZenCircleScreen

/**
 * Hosts the Zen Bro "Circle" stage's [ZenCircleScreen] -- split out of MainActivity (at the
 * repo's line-count ceiling) the same way BuddyConnector/CircleStageMembers were. See
 * [buildCircleStageMembers] for the three member-list cases this renders.
 */
@Composable
fun CircleStageScreen(
    circle: Circle?,
    isCircleMode: Boolean,
    buddyNameFallback: String,
    userCode: String?,
    usage: DailyUsage?,
    zenScore: Int,
    streakCount: Int,
    yesterdayChangePercent: Int?,
    buddyStats: BuddyStats?,
    reactions: Pair<Long, Long>,
    connectedToBuddyScreen: Boolean,
    removingBuddy: Boolean,
    circleRemoving: Boolean,
    buddyConnector: BuddyConnector,
    circleViewModel: CircleViewModel,
    viewModel: MainViewModel,
    context: Context,
    onBackToZenCircleFalse: () -> Unit,
    onCloseBuddyConnect: () -> Unit,
    onRemoveBuddy: () -> Unit
) {
    val members = buildCircleStageMembers(
        realCircle = circle,
        isCircleMode = isCircleMode,
        buddyNameFallback = buddyNameFallback,
        userCode = userCode,
        usage = usage,
        zenScore = zenScore,
        streakCount = streakCount,
        yesterdayChangePercent = yesterdayChangePercent,
        buddyStats = buddyStats,
        reactions = reactions
    )
    // Resolved once, here, where circle-vs-buddy state is actually known -- everything below
    // (the Invite sheet, the share card) just displays/shares whatever this resolves to,
    // without re-deriving which mode the user is in.
    val shareCode = circle?.id ?: userCode
    ZenCircleScreen(
        members = members,
        shareCode = shareCode,
        shareCodeIsCircle = circle != null,
        primaryShareLabel = if (circle != null && circle.members.size < 2) "Remind With Share Link" else "Share & Invite to Zen Circle",
        // Opened from home there's no connected screen to return to.
        onBackClick = { if (connectedToBuddyScreen) onBackToZenCircleFalse() else onCloseBuddyConnect() },
        onShareInviteLink = {
            // A real circle shares its own /c/{circleId} join link, not the classic /b/ buddy
            // link the reskin fallback uses. Still creating (isCircleMode, circle null):
            // nothing to share yet -- MainActivity's justEnteredCircle effect fires this once ready.
            when {
                circle != null -> buddyConnector.shareCircleInvite(circle.id)
                isCircleMode -> Unit
                else -> userCode?.let { buddyConnector.shareBuddyInvite(it) }
            }
        },
        onCopyInviteCode = {
            when {
                circle != null -> buddyConnector.copyUserCode(circle.id, showToast = true)
                isCircleMode -> Unit
                else -> userCode?.let { buddyConnector.copyUserCode(it, showToast = false) }
            }
        },
        onBackToHome = onCloseBuddyConnect,
        onSendLove = { member ->
            when {
                // The dummy "invite pending" slot has no uid -- nothing to react to yet.
                circle != null && member.uid.isBlank() -> Unit
                circle != null -> circleViewModel.sendReaction(member.uid, ReactionType.LOVE)
                else -> viewModel.sendLike()
            }
        },
        onSendMelt = { member ->
            when {
                circle != null && member.uid.isBlank() -> Unit
                circle != null -> circleViewModel.sendReaction(member.uid, ReactionType.MELT)
                else -> Toast.makeText(context, "Sad-face reactions arrive with Zen Circles", Toast.LENGTH_SHORT).show()
            }
        },
        onWeeklyClick = {
            Toast.makeText(context, "Weekly rankings are part of PRO", Toast.LENGTH_SHORT).show()
        },
        removingBuddy = removingBuddy || circleRemoving,
        // "Remove buddy" on a real circle was wired straight to the classic 1:1 removeBuddy(),
        // which checks the classic buddy-UID cache -- always empty for a circle-only user, so
        // it silently failed with "you don't have a Zen Bro to remove" even though the dialog's
        // own copy ("End your Zen Bro connection with X") clearly means the circle relationship.
        // Only the leader may kick the other member (Firestore rules leave that enforcement to
        // app logic, see firestore.rules' circles/update comment) -- a non-leader tapping this
        // can only end their own membership, same outcome Leave Circle already gives them.
        onRemoveBuddy = {
            val myUid = ServiceLocator.authProvider.getCurrentUserId()
            when {
                circle == null -> onRemoveBuddy()
                circle.leaderUid == myUid -> {
                    circle.members.firstOrNull { it.uid != myUid }?.uid?.let { circleViewModel.removeMember(it) }
                }
                else -> circleViewModel.leaveCircle()
            }
        },
        onLeaveCircle = {
            if (circle != null) circleViewModel.leaveCircle() else onRemoveBuddy()
        }
    )
}
