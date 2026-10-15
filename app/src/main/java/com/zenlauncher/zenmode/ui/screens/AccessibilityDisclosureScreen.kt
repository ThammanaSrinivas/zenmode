package com.zenlauncher.zenmode.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.zenlauncher.zenmode.ui.components.WeightSpacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.Spacing
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

/**
 * Play-policy prominent disclosure, shown before sending the user to Accessibility settings.
 */
@Composable
fun AccessibilityDisclosureScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .padding(horizontal = Spacing.screenMargin)
            .padding(top = 48.rdp, bottom = 24.rdp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = stringResource(R.string.a11y_disclosure_title),
                color = colors.textPrimary,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.rsp
                )
            )

            Spacer(modifier = Modifier.height(24.rdp))

            // Section: Why
            Text(
                text = stringResource(R.string.a11y_disclosure_why, PRODUCT_NAME),
                color = colors.textBrand,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = stringResource(R.string.a11y_disclosure_why_body, PRODUCT_NAME),
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Section: What it does
            Text(
                text = stringResource(R.string.a11y_disclosure_does_title),
                color = colors.textBrand,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = stringResource(R.string.a11y_disclosure_does_body),
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Section: What it does NOT do
            Text(
                text = stringResource(R.string.a11y_disclosure_not_does_title),
                color = colors.textBrand,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = stringResource(R.string.a11y_disclosure_not_does_body),
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Section: Privacy
            Text(
                text = stringResource(R.string.a11y_disclosure_privacy_title),
                color = colors.textBrand,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = stringResource(R.string.a11y_disclosure_privacy_body),
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = stringResource(R.string.a11y_disclosure_read_policy),
                color = colors.textBrand,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PRIVACY_POLICY_URL))
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.rdp))
        }

        // Buttons at the bottom
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.textBrand),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(R.string.a11y_disclosure_accept),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.rdp))

        TextButton(
            onClick = onDecline,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(R.string.a11y_disclosure_decline), color = colors.textSecondary)
        }
    }
}
