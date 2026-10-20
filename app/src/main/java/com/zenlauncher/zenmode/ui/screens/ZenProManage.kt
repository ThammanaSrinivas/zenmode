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
import androidx.compose.ui.res.stringResource
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
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
import com.zenlauncher.zenmode.R

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
        ZenEyebrow(stringResource(R.string.common_pro_supporter) + (entitlement.since?.let { stringResource(R.string.zen_pro_manage_supporter_since, it.asZenMonth()) } ?: ""))
        Text(
            text = stringResource(R.string.common_early_access),
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.rsp,
            lineHeight = 28.rsp,
            color = colors.textPrimary
        )
        Text(
            text = stringResource(R.string.zen_pro_manage_granted_body),
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
    val unit = if (entitlement.period == BillingPeriod.MONTHLY) stringResource(R.string.common_unit_month) else stringResource(R.string.common_unit_year)

    Column(
        modifier = Modifier.fillMaxWidth().staggeredEntrance(0).zenCard().padding(16.rdp),
        verticalArrangement = Arrangement.spacedBy(6.rdp)
    ) {
        OsRule()
        Spacer(Modifier.height(6.rdp))
        ZenEyebrow(stringResource(R.string.common_pro_supporter) + (entitlement.since?.let { stringResource(R.string.zen_pro_manage_supporter_since, it.asZenMonth()) } ?: ""))
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
                stringResource(R.string.zen_pro_manage_status_label),
                when (entitlement.status) {
                    ProStatus.TRIAL -> stringResource(R.string.common_free_trial)
                    ProStatus.ENDING -> stringResource(R.string.zen_pro_manage_status_ending)
                    else -> stringResource(R.string.zen_pro_manage_status_active)
                }
            )
            when (entitlement.status) {
                ProStatus.TRIAL -> ZenReceiptLine(
                    if (isSimulated) stringResource(R.string.zen_pro_manage_trial_ends_label) else stringResource(R.string.zen_pro_manage_first_charge_label),
                    entitlement.trialEndsOn?.asZenDate() ?: stringResource(R.string.common_dash_placeholder)
                )
                ProStatus.ENDING -> ZenReceiptLine(stringResource(R.string.zen_pro_manage_pro_until_label), entitlement.endsOn?.asZenDate() ?: stringResource(R.string.common_dash_placeholder))
                else -> ZenReceiptLine(stringResource(R.string.zen_pro_manage_renews_label), entitlement.renewsOn?.asZenDate() ?: stringResource(R.string.common_dash_placeholder))
            }
            ZenReceiptLine(
                stringResource(R.string.common_then),
                when {
                    ending -> stringResource(R.string.zen_pro_manage_back_to_free)
                    isSimulated -> stringResource(R.string.zen_pro_manage_no_charge_early_access)
                    else -> offer?.let { "${it.formattedPrice} a $unit" } ?: "-"
                },
                showDivider = false
            )
        }
    }

    Text(
        text = when {
            isSimulated && ending ->
                stringResource(R.string.zen_pro_manage_ending_note)
            isSimulated ->
                stringResource(R.string.zen_pro_manage_early_note_part_one) +
                    stringResource(R.string.zen_pro_manage_early_note_part_two)
            // Real billing: Play, not this screen, is the source of truth for cancel/resume --
            // no app can act on a subscription on the user's behalf, only Play's own UI can.
            else ->
                stringResource(R.string.zen_pro_manage_play_store_note_one) +
                    stringResource(R.string.zen_pro_manage_play_store_note_two, PRODUCT_NAME)
        },
        fontFamily = Geist,
        fontSize = 15.rsp,
        lineHeight = 22.rsp,
        color = colors.textSecondary
    )

    if (isSimulated) {
        if (ending) {
            ZenButton(text = if (isWorking) stringResource(R.string.zen_pro_manage_resuming_button) else stringResource(R.string.zen_pro_manage_resume_button), onClick = onResume, enabled = !isWorking)
        } else {
            ZenButton(
                text = if (isWorking) stringResource(R.string.zen_pro_manage_cancelling_button) else stringResource(R.string.common_cancel_pro),
                onClick = { confirmCancel = true },
                style = ZenButtonStyle.Outline,
                enabled = !isWorking
            )
        }
    } else {
        // One button either way: cancel and resume are the same Play Store screen, and we
        // can't promise the tap did anything until Play's own RTDN reconciles state back to us.
        ZenButton(
            text = stringResource(R.string.zen_pro_manage_play_store_button),
            onClick = if (ending) onResume else onCancel,
            style = ZenButtonStyle.Outline,
            enabled = !isWorking
        )
    }

    if (isSimulated && confirmCancel) {
        val until = (if (entitlement.status == ProStatus.TRIAL) entitlement.trialEndsOn else entitlement.renewsOn)
            ?.asZenDate()
        ZenSheet(onDismiss = { confirmCancel = false }) {
            ZenSheetTitle(stringResource(R.string.zen_pro_manage_cancel_sheet_title))
            ZenSheetBody(
                stringResource(R.string.zen_pro_manage_cancel_sheet_body_one, until ?: stringResource(R.string.zen_pro_manage_end_of_period)) +
                    stringResource(R.string.zen_pro_manage_cancel_sheet_body_two)
            )
            ZenSheetBody(stringResource(R.string.zen_pro_manage_cancel_sheet_history))
            // Same weight on purpose: no retention offer, no "are you sure" chain.
            Row(horizontalArrangement = Arrangement.spacedBy(8.rdp)) {
                ZenButton(
                    text = stringResource(R.string.common_cancel_pro),
                    onClick = {
                        confirmCancel = false
                        onCancel()
                    },
                    style = ZenButtonStyle.Outline,
                    modifier = Modifier.weight(1f)
                )
                ZenButton(
                    text = stringResource(R.string.zen_pro_manage_keep_pro_button),
                    onClick = { confirmCancel = false },
                    style = ZenButtonStyle.Outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
