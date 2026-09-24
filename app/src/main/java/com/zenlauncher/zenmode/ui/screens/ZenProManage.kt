package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.zenlauncher.zenmode.coreapi.services.BillingPeriod
import com.zenlauncher.zenmode.coreapi.services.Entitlement
import com.zenlauncher.zenmode.coreapi.services.PlanOffer
import com.zenlauncher.zenmode.coreapi.services.ProStatus
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.components.ZenButton
import com.zenlauncher.zenmode.ui.components.ZenButtonStyle
import com.zenlauncher.zenmode.ui.components.ZenEyebrow
import com.zenlauncher.zenmode.ui.components.ZenReceiptLine
import com.zenlauncher.zenmode.ui.components.ZenSheet
import com.zenlauncher.zenmode.ui.components.ZenSheetBody
import com.zenlauncher.zenmode.ui.components.ZenSheetTitle
import com.zenlauncher.zenmode.ui.components.zenCard
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

// ZenMode Pro -- the manage page (a real subscription) and the granted page (invite-only,
// nothing to manage). Split out of ZenProScreen.kt, which hosts both; the sales rules at the
// top of that file apply here too.

/** Invite-only server grant, no subscription behind it: nothing to cancel or renew. */
@Composable
internal fun GrantedPro(entitlement: Entitlement) {
    val colors = ZenTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().zenCard().padding(16.rdp),
        verticalArrangement = Arrangement.spacedBy(6.rdp)
    ) {
        OsRule()
        Spacer(Modifier.height(6.rdp))
        ZenEyebrow("Pro supporter" + (entitlement.since?.let { " · since ${it.asZenMonth()}" } ?: ""))
        Text(
            text = "Early access",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.rsp,
            lineHeight = 28.rsp,
            color = colors.textPrimary
        )
        Text(
            text = "You're in on invite-only early access — nothing to manage or pay here.",
            fontFamily = Geist,
            fontSize = 14.rsp,
            lineHeight = 20.rsp,
            color = colors.textSecondary
        )
    }
}

@Composable
internal fun ManagePro(
    entitlement: Entitlement,
    offers: List<PlanOffer>,
    isWorking: Boolean,
    isSimulated: Boolean,
    onCancel: () -> Unit,
    onResume: () -> Unit
) {
    val colors = ZenTheme.colors
    var confirmCancel by remember { mutableStateOf(false) }
    val ending = entitlement.status == ProStatus.ENDING
    val offer = entitlement.period?.let { offers.offer(it) }
    val unit = if (entitlement.period == BillingPeriod.MONTHLY) "month" else "year"

    Column(
        modifier = Modifier.fillMaxWidth().staggeredEntrance(0).zenCard().padding(16.rdp),
        verticalArrangement = Arrangement.spacedBy(6.rdp)
    ) {
        OsRule()
        Spacer(Modifier.height(6.rdp))
        ZenEyebrow("Pro supporter" + (entitlement.since?.let { " · since ${it.asZenMonth()}" } ?: ""))
        Text(
            text = buildString {
                append(if (entitlement.period == BillingPeriod.MONTHLY) "Monthly" else "Annual")
                if (offer != null) append(" · ${offer.formattedPrice}/$unit")
            },
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.rsp,
            lineHeight = 28.rsp,
            color = colors.textPrimary
        )
        Column {
            ZenReceiptLine(
                "Status",
                when (entitlement.status) {
                    ProStatus.TRIAL -> "Free trial"
                    ProStatus.ENDING -> "Ending"
                    else -> "Active"
                }
            )
            when (entitlement.status) {
                ProStatus.TRIAL -> ZenReceiptLine(
                    if (isSimulated) "Trial ends" else "First charge",
                    entitlement.trialEndsOn?.asZenDate() ?: "-"
                )
                ProStatus.ENDING -> ZenReceiptLine("Pro until", entitlement.endsOn?.asZenDate() ?: "-")
                else -> ZenReceiptLine("Renews", entitlement.renewsOn?.asZenDate() ?: "-")
            }
            ZenReceiptLine(
                "Then",
                when {
                    ending -> "Back to free"
                    isSimulated -> "No charge, early access"
                    else -> offer?.let { "${it.formattedPrice} a $unit" } ?: "-"
                },
                showDivider = false
            )
        }
    }

    Text(
        text = when {
            isSimulated && ending ->
                "You're on early access, so nothing was charged. Resume any time before Pro ends."
            isSimulated ->
                "You're on early access: nothing is charged. When paid plans open we'll ask " +
                    "first, and your Pro carries on until then."
            // Real billing: Play, not this screen, is the source of truth for cancel/resume --
            // no app can act on a subscription on the user's behalf, only Play's own UI can.
            else ->
                "Renewals, cancellations and refunds are all managed in the Play Store, not " +
                    "here. Changes made there can take a few minutes to show up in ZenMode."
        },
        fontFamily = Geist,
        fontSize = 15.rsp,
        lineHeight = 22.rsp,
        color = colors.textSecondary
    )

    if (isSimulated) {
        if (ending) {
            ZenButton(text = if (isWorking) "Resuming…" else "Resume Pro", onClick = onResume, enabled = !isWorking)
        } else {
            ZenButton(
                text = if (isWorking) "Cancelling…" else "Cancel Pro",
                onClick = { confirmCancel = true },
                style = ZenButtonStyle.Outline,
                enabled = !isWorking
            )
        }
    } else {
        // One button either way: cancel and resume are the same Play Store screen, and we
        // can't promise the tap did anything until Play's own RTDN reconciles state back to us.
        ZenButton(
            text = "Manage in Play Store",
            onClick = if (ending) onResume else onCancel,
            style = ZenButtonStyle.Outline,
            enabled = !isWorking
        )
    }

    if (isSimulated && confirmCancel) {
        val until = (if (entitlement.status == ProStatus.TRIAL) entitlement.trialEndsOn else entitlement.renewsOn)
            ?.asZenDate()
        ZenSheet(onDismiss = { confirmCancel = false }) {
            ZenSheetTitle("Cancel Pro?")
            ZenSheetBody(
                "Pro runs until ${until ?: "the end of this period"}, then the app goes back to free. " +
                    "The launcher, intent, sessions, Zen Score, your daily report and Zen Circle all stay."
            )
            ZenSheetBody("History past 7 days stops showing. None of it is deleted.")
            // Same weight on purpose: no retention offer, no "are you sure" chain.
            Row(horizontalArrangement = Arrangement.spacedBy(8.rdp)) {
                ZenButton(
                    text = "Cancel Pro",
                    onClick = {
                        confirmCancel = false
                        onCancel()
                    },
                    style = ZenButtonStyle.Outline,
                    modifier = Modifier.weight(1f)
                )
                ZenButton(
                    text = "Keep Pro",
                    onClick = { confirmCancel = false },
                    style = ZenButtonStyle.Outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
