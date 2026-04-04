package com.zenlauncher.zenmode.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp

@Composable
fun ZenShoutoutScreen(onNextClick: () -> Unit, onBackClick: () -> Unit) {
    val colors = ZenTheme.colors
    OnboardingScreenLayout(
        progress = 0.33f,
        progressText = "33%",
        buttonText = "Let's Goooo!",
        onButtonClick = onNextClick,
        showLogo = true,
        onBackClick = onBackClick,
        showBgShuriken = true,
        bgShurikenOffsetX = (-80).rdp,
        bgShurikenOffsetY = 50.rdp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.rdp)
                .padding(top = 16.rdp, bottom = 24.rdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // heart_sharukhan icon above the title
            Image(
                painter = painterResource(id = R.drawable.heart_sharukhan),
                contentDescription = null,
                modifier = Modifier.size(72.rdp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.rdp))

            Text(
                text = "Zenmode",
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "An Open-source minimalist android launcher",
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(
                        color = colors.textPrimary,
                        offset = Offset(0f, 4f),
                        blurRadius = 20f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.rdp))

            // Shoutout to Sharukhan
            val sharukhanText = buildAnnotatedString {
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textPrimary)) {
                    append("Shout out, ")
                }
                withStyle(
                    MaterialTheme.typography.bodyLarge.toSpanStyle().copy(
                        color = colors.textBrand,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append("Sharukhan")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textPrimary)) {
                    append(" our early Zen who played important in building zenmode. You can write to us at ")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textBrand)) {
                    append("zenmode.help@gmail.com")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textPrimary)) {
                    append(" to get featured here")
                }
            }
            Text(
                text = sharukhanText,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.rdp))

            // Mission statement
            val missionText = buildAnnotatedString {
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textPrimary)) {
                    append("It's our way of showing love towards world, ")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textBrand)) {
                    append("We are super happy, If we could make 1% people break their smartphone addiction, ")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = colors.textPrimary)) {
                    append("With truckloads of love")
                }
            }
            Text(
                text = missionText,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "-Srini & Kamal",
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.rdp))

            // thumbs_up + "Help us build Zenmode, Github (↗)"
            val uriHandler = LocalUriHandler.current
            val githubText = buildAnnotatedString {
                withStyle(
                    MaterialTheme.typography.titleMedium.toSpanStyle().copy(color = colors.textPrimary)
                ) {
                    append("Help us build Zenmode, ")
                }
                pushStringAnnotation(tag = "URL", annotation = AppConstants.GITHUB_URL)
                withStyle(
                    MaterialTheme.typography.titleMedium.toSpanStyle().copy(
                        color = colors.textBrand,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Github (↗)")
                }
                pop()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.thumbs_up),
                    contentDescription = null,
                    modifier = Modifier.size(28.rdp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.text.ClickableText(
                    text = githubText,
                    onClick = { offset ->
                        githubText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { uriHandler.openUri(it.item) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "(You can star us, believe me we won't feel bad if not, iykyk)",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp)
            )
        }
    }
}

class ZenShoutoutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                ZenTheme {
                    ZenShoutoutScreen(
                        onNextClick = { navigateTo(+1) },
                        onBackClick = { navigateTo(-1) }
                    )
                }
            }
        }
    }

    private fun navigateTo(delta: Int) {
        if (delta > 0) {
            val repository = com.zenlauncher.zenmode.coreapi.UsageRepository(
                requireContext(),
                com.zenlauncher.zenmode.coreapi.services.ServiceLocator.analyticsManager
            )
            if (!repository.isOnboardingStartedTracked()) {
                com.zenlauncher.zenmode.coreapi.services.ServiceLocator.analyticsTracker.trackOnboardingStarted()
                repository.setOnboardingStartedTracked(true)
            }
        }
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager) ?: return
        viewPager.currentItem = (viewPager.currentItem + delta).coerceAtLeast(0)
    }
}
