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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
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
                text = "Accessibility Service Disclosure",
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
                text = "Why ZenMode needs Accessibility Service",
                color = colors.textBrand,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = "ZenMode uses Android's Accessibility Service API for two features:\n\n" +
                        "1. Lock screen — Android gives a launcher no other way to lock the screen, " +
                        "so this permission is required for the lock button to work.\n\n" +
                        "2. In-app content blocking (optional) — when you turn on blocking for a " +
                        "surface such as YouTube Shorts, the service checks " +
                        "whether that screen is currently open and, if so, navigates you away. It runs " +
                        "only for apps you have explicitly chosen to block.",
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Section: What it does
            Text(
                text = "What this service does",
                color = colors.textBrand,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = "• Uses the system lock-screen action (GLOBAL_ACTION_LOCK_SCREEN) when you tap the lock button\n" +
                        "• For apps you chose to block: checks which screen (view IDs) is visible and, on a blocked screen, presses Back\n" +
                        "• Only wakes for apps in your block list (currently YouTube)",
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Section: What it does NOT do
            Text(
                text = "What this service does NOT do",
                color = colors.textBrand,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = "• Does NOT read, store, or transmit the text, media, or messages inside any app\n" +
                        "• Does NOT monitor apps that are not in your block list\n" +
                        "• Does NOT perform gestures or tap anything inside other apps\n" +
                        "• Does NOT collect, store, transmit, or share any personal data\n" +
                        "• Sends nothing off your device — all checks happen locally",
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Section: Privacy
            Text(
                text = "Privacy",
                color = colors.textBrand,
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.rsp
                )
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = "This service enables the lock button and, if you turn it on, in-app content blocking. " +
                        "It reads only which screen of a blocked app is visible, on the device, to decide " +
                        "whether to navigate away. No screen content or personal data is stored, collected, or sent anywhere.",
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.rdp))
            Text(
                text = "Read our Privacy Policy",
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
                text = "I Understand and Agree",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.rdp))

        TextButton(
            onClick = onDecline,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Decline", color = colors.textSecondary)
        }
    }
}
