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
import com.zenlauncher.zenmode.ui.theme.Grey400
import com.zenlauncher.zenmode.ui.theme.White
import com.zenlauncher.zenmode.ui.theme.ZenBase
import com.zenlauncher.zenmode.ui.theme.ZenGlow
import com.zenlauncher.zenmode.ui.theme.ZenTheme

@Composable
fun ZenShoutoutScreen(onNextClick: () -> Unit, onBackClick: () -> Unit) {
    OnboardingScreenLayout(
        progress = 0.33f,
        progressText = "33%",
        buttonText = "Let's Goooo!",
        onButtonClick = onNextClick,
        showLogo = true,
        onBackClick = onBackClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // heart_sharukhan icon above the title
            Image(
                painter = painterResource(id = R.drawable.heart_sharukhan),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Zenmode",
                color = White,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "An Open-source minimalist android launcher",
                color = White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Shoutout to Sharukhan
            val sharukhanText = buildAnnotatedString {
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = White)) {
                    append("Shout out, ")
                }
                withStyle(
                    MaterialTheme.typography.bodyLarge.toSpanStyle().copy(
                        color = ZenGlow,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append("Sharukhan")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = White)) {
                    append(" our early Zen who played important in building zenmode. You can write to us at ")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = ZenBase)) {
                    append("zenmode.help@gmail.com")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = White)) {
                    append(" to get featured here")
                }
            }
            Text(
                text = sharukhanText,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mission statement
            val missionText = buildAnnotatedString {
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = White)) {
                    append("It's our way of showing love towards world, ")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = ZenBase)) {
                    append("We are super happy, If we could make 1% people break their smartphone addiction, ")
                }
                withStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = White)) {
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
                color = White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // thumbs_up + "Help us build Zenmode, Github (↗)"
            val uriHandler = LocalUriHandler.current
            val githubText = buildAnnotatedString {
                withStyle(
                    MaterialTheme.typography.titleMedium.toSpanStyle().copy(color = White)
                ) {
                    append("Help us build Zenmode, ")
                }
                pushStringAnnotation(tag = "URL", annotation = AppConstants.GITHUB_URL)
                withStyle(
                    MaterialTheme.typography.titleMedium.toSpanStyle().copy(
                        color = ZenGlow,
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
                    modifier = Modifier.size(28.dp),
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
                color = Grey400,
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
