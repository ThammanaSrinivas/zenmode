package com.zenlauncher.zenmode.onboarding

import androidx.compose.ui.text.TextStyle
import com.zenlauncher.zenmode.ui.components.BrandedText
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.ZenModeOsWordmark
import com.zenlauncher.zenmode.ui.components.pressScale
import com.zenlauncher.zenmode.ui.components.staggeredEntrance
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.delay

/**
 * 01 · Welcome. The cover, before any asks: why ZenMode exists, proof people like it,
 * and the humans building it in the open. Warm paper and a sunrise wash — closer to a
 * handwritten letter than a sign-up form.
 */
@Composable
internal fun WelcomeStep(
    isReturningUser: Boolean,
    onContinue: () -> Unit
) {
    val sunrise = colorResource(R.color.onboarding_sunrise)
    val paper = colorResource(R.color.paper_base)

    OnboardingPage(
        modifier = Modifier.background(Brush.verticalGradient(0f to sunrise, 0.55f to paper)),
        background = Color.Transparent,
        bottomBar = {
            Text(
                text = "Quiet the noise, Together.",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 17.rsp,
                letterSpacing = (-0.2).sp,
                color = colorResource(R.color.zen_900),
                modifier = Modifier.staggeredEntrance(6)
            )
            OnboardingButton(
                text = if (isReturningUser) "Update now" else "Let's begin",
                onClick = onContinue,
                modifier = Modifier.staggeredEntrance(7)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OnboardingMargin)
        ) {
            BrandRow(modifier = Modifier.padding(top = 18.rdp).staggeredEntrance(0))

            Spacer(Modifier.height(if (isReturningUser) 20.rdp else 36.rdp))
            if (isReturningUser) {
                RevampBanner(modifier = Modifier.staggeredEntrance(1))
                Spacer(Modifier.height(24.rdp))
            }

            OnboardingEyebrow("Our mission", modifier = Modifier.staggeredEntrance(1))
            Spacer(Modifier.height(12.rdp))
            MissionHeadline(modifier = Modifier.staggeredEntrance(2))
            Spacer(Modifier.height(14.rdp))
            OnboardingBody(
                text = "ZenMode OS is a calmer home screen that helps you use your phone on purpose — " +
                    "and brings the people you love along.",
                modifier = Modifier.staggeredEntrance(3)
            )

            Spacer(Modifier.height(28.rdp))
            ReviewsCard(modifier = Modifier.staggeredEntrance(4))

            Spacer(Modifier.height(14.rdp))
            OpenSourceCard(modifier = Modifier.staggeredEntrance(5))
            Spacer(Modifier.height(12.rdp))
        }
    }
}

@Composable
private fun BrandRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ZenModeOsWordmark(
            fontSize = 21.rsp,
            markSize = 26.rdp,
            markGap = 10.rdp,
            modifier = Modifier.weight(1f)
        )
        OnboardingChip(
            text = "Open source",
            container = Color.White.copy(alpha = 0.7f),
            content = colorResource(R.color.ink_surface),
            leading = {
                Box(
                    Modifier
                        .size(6.rdp)
                        .clip(CircleShape)
                        .background(colorResource(R.color.zen_500))
                )
            }
        )
    }
}

/** For v2 users arriving after the update: "we've something special for you". */
@Composable
private fun RevampBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.rdp))
            .background(colorResource(R.color.zen_900))
            .padding(horizontal = 16.rdp, vertical = 14.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "✦", fontSize = 18.rsp, color = colorResource(R.color.amber_500))
        Spacer(Modifier.width(12.rdp))
        Column {
            Text(
                text = "We've something special for you",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.rsp,
                color = Color.White
            )
            BrandedText(
                text = "ZenMode is now ZenMode OS. A two-minute tour and you're in.",
                style = TextStyle(
                    fontFamily = Geist,
                    fontSize = 13.rsp,
                    lineHeight = 18.rsp,
                    color = colorResource(R.color.zen_100)
                )
            )
        }
    }
}

@Composable
private fun MissionHeadline(modifier: Modifier = Modifier) {
    val ink = colorResource(R.color.ink_surface)
    val green = colorResource(R.color.zen_700)
    Text(
        text = buildAnnotatedString {
            append("We can't live without our phones.\n")
            withStyle(SpanStyle(color = green)) { append("So let's make it a kinder relationship.") }
        },
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 34.rsp,
        lineHeight = 37.rsp,
        letterSpacing = (-0.7).sp,
        color = ink,
        modifier = modifier
    )
}

// ── Reviews ───────────────────────────────────────────────────────

@Composable
private fun ReviewsCard(modifier: Modifier = Modifier) {
    val reviews = OnboardingContent.reviews
    var index by remember { mutableIntStateOf(0) }
    if (reviews.size > 1) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(4_500)
                index = (index + 1) % reviews.size
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.rdp))
            .background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, colorResource(R.color.paper_hairline), RoundedCornerShape(22.rdp))
            .padding(18.rdp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = colorResource(R.color.amber_500),
                    modifier = Modifier.size(16.rdp)
                )
            }
            Spacer(Modifier.width(8.rdp))
            Text(
                text = "${AppConstants.PLAY_RATING} on Google Play",
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.rsp,
                color = colorResource(R.color.ink_surface),
                modifier = Modifier.semantics { contentDescription = "Rated ${AppConstants.PLAY_RATING} out of 5 on Google Play" }
            )
        }

        if (reviews.isNotEmpty()) {
            Spacer(Modifier.height(12.rdp))
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 4 })
                        .togetherWith(fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 4 })
                },
                label = "review"
            ) { i ->
                val review = reviews[i]
                Column(modifier = Modifier.heightIn(min = 76.rdp)) {
                    Text(
                        text = "“${review.quote}”",
                        fontFamily = Geist,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.rsp,
                        lineHeight = 23.rsp,
                        color = colorResource(R.color.ink_surface)
                    )
                    Spacer(Modifier.height(6.rdp))
                    Text(
                        text = "— ${review.author}",
                        fontFamily = Geist,
                        fontSize = 13.rsp,
                        color = colorResource(R.color.stone_500)
                    )
                }
            }
            Spacer(Modifier.height(10.rdp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.rdp)) {
                reviews.indices.forEach { i ->
                    Box(
                        Modifier
                            .height(4.rdp)
                            .width(if (i == index) 16.rdp else 4.rdp)
                            .clip(CircleShape)
                            .background(
                                if (i == index) colorResource(R.color.zen_700)
                                else colorResource(R.color.paper_hairline)
                            )
                    )
                }
            }
        } else {
            Spacer(Modifier.height(6.rdp))
            Text(
                text = "Loved by early Zens who wanted their evenings back.",
                fontFamily = Geist,
                fontSize = 15.rsp,
                lineHeight = 21.rsp,
                color = colorResource(R.color.stone_600)
            )
        }
    }
}

// ── Open source ───────────────────────────────────────────────────

@Composable
private fun OpenSourceCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val contributors = OnboardingContent.contributors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.rdp))
            .background(colorResource(R.color.ink_surface))
            .padding(18.rdp)
    ) {
        OnboardingEyebrow("Built in the open", color = colorResource(R.color.zen_300))
        Spacer(Modifier.height(14.rdp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Overlapping avatar stack, then the empty "you" seat.
            Box(modifier = Modifier.width((40 + 30 * contributors.size).rdp).height(40.rdp)) {
                contributors.forEachIndexed { i, person ->
                    ContributorAvatar(
                        person = person,
                        modifier = Modifier.offset(x = (30 * i).rdp)
                    )
                }
                YourSeat(modifier = Modifier.offset(x = (30 * contributors.size).rdp))
            }
            Spacer(Modifier.width(12.rdp))
            Text(
                text = buildAnnotatedString {
                    append("Shout out to ")
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
                        append(contributors.joinToString(", ") { it.name })
                    }
                    append(". ")
                    withStyle(SpanStyle(color = colorResource(R.color.amber_500), fontWeight = FontWeight.SemiBold)) {
                        append("Your name next?")
                    }
                },
                fontFamily = Geist,
                fontSize = 13.rsp,
                lineHeight = 18.rsp,
                color = colorResource(R.color.stone_300),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.rdp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.rdp)) {
            LinkPill("GitHub", modifier = Modifier.weight(1f)) { uriHandler.openUri(AppConstants.GITHUB_URL) }
            LinkPill("Telegram", modifier = Modifier.weight(1f)) { uriHandler.openUri(AppConstants.TELEGRAM_URL) }
        }
    }
}

@Composable
private fun ContributorAvatar(person: OnboardingContent.Contributor, modifier: Modifier = Modifier) {
    val ring = colorResource(R.color.ink_surface)
    val initials = @Composable {
        Box(
            Modifier
                .fillMaxSize()
                .background(colorResource(R.color.zen_700)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = person.initials,
                fontFamily = DepartureMono,
                fontSize = 11.rsp,
                color = Color.White
            )
        }
    }
    Box(
        modifier = modifier
            .size(40.rdp)
            .clip(CircleShape)
            .border(2.dp, ring, CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
    ) {
        if (person.githubLogin != null) {
            SubcomposeAsyncImage(
                model = "https://avatars.githubusercontent.com/${person.githubLogin}?s=96",
                contentDescription = person.name,
                loading = { initials() },
                error = { initials() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            initials()
        }
    }
}

@Composable
private fun YourSeat(modifier: Modifier = Modifier) {
    val dash = colorResource(R.color.amber_500)
    Box(
        modifier = modifier
            .size(40.rdp)
            .clip(CircleShape)
            .background(colorResource(R.color.ink_base)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = dash,
                radius = size.minDimension / 2 - 2.dp.toPx(),
                center = Offset(size.width / 2, size.height / 2),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            )
        }
        Text(text = "+", fontFamily = Geist, fontSize = 18.rsp, color = dash)
    }
}

@Composable
private fun LinkPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(44.rdp)
            .pressScale(onClick = onClick, onClickLabel = "Open $text", pressedScale = 0.96f)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(percent = 50)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.rsp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(6.rdp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(14.rdp)
        )
    }
}
