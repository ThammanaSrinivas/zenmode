package com.zenlauncher.zenmode.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.AppConstants
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.ClashLineHeight
import com.zenlauncher.zenmode.ui.components.FullLineBox
import com.zenlauncher.zenmode.ui.components.GeistLineHeight
import com.zenlauncher.zenmode.ui.components.V3BrandGreen
import com.zenlauncher.zenmode.ui.components.V3BulletDot
import com.zenlauncher.zenmode.ui.components.V3CardDivider
import com.zenlauncher.zenmode.ui.components.V3PillButtonText
import com.zenlauncher.zenmode.ui.components.V3PrimaryPillButton
import com.zenlauncher.zenmode.ui.components.V3RollingNumber
import com.zenlauncher.zenmode.ui.components.V3ScreenHeader
import com.zenlauncher.zenmode.ui.components.V3ValueStepper
import com.zenlauncher.zenmode.ui.components.v3GradientCard
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ── ZM_OS v3: My Promise ────────────────────────────────────────────
// Figma node 2026:1793. Opened from Zen Gold's "Edit my promise". Light-only by
// design (no dark variant exists), so colors come straight from colors.xml tokens
// rather than ZenTheme.colors. Vertical rhythm reproduces the frame's absolute
// offsets; any extra screen height goes between the note and the footer.

private val ContentMargin: Dp @Composable get() = 32.rdp
private val HeadingMargin: Dp @Composable get() = 33.rdp

private val BrandGreen: Color @Composable get() = V3BrandGreen

// Geist's line box equals its font height in DailyEquivalentLine, so its baseline is this ratio.
private const val GeistAscentRatio = 1.005f / GeistLineHeight

@Composable
fun MyPromiseScreen(
    dailyHours: Int,
    onDailyHoursChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onSeeAllHoldingsClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.my_promise_bg))
    ) {
        Image(
            painter = painterResource(R.drawable.bg_my_promise_glow),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(34.rdp))
                    V3ScreenHeader(title = "My Promise", backLabel = "Back to Zen Gold", onBackClick = onBackClick)
                    Spacer(modifier = Modifier.height(45.rdp))
                    IntroText()
                    Spacer(modifier = Modifier.height(13.86.rdp))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = ContentMargin)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.rdp)
                    ) {
                        PromiseCard(dailyHours = dailyHours, onDailyHoursChange = onDailyHoursChange)
                        PromiseRulesNote(dailyHours = dailyHours)
                    }
                }

                Column {
                    Spacer(modifier = Modifier.height(38.rdp))
                    BrandStrip()
                    Spacer(modifier = Modifier.height(21.rdp))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 31.5.rdp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.rdp)
                    ) {
                        V3PrimaryPillButton(text = "Back to Zen Gold", onClick = onBackClick)
                        OutlinedPillButton(text = "See all holdings", onClick = onSeeAllHoldingsClick)
                    }
                    Spacer(modifier = Modifier.height(45.rdp))
                }
            }
        }
    }
}

@Composable
private fun IntroText() {
    Text(
        text = "Set your weekly promise",
        fontFamily = ClashDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 28.rsp,
        lineHeight = (28 * ClashLineHeight).rsp,
        letterSpacing = (-0.84).sp,
        color = Color.Black,
        style = FullLineBox,
        modifier = Modifier
            .padding(horizontal = HeadingMargin)
            .semantics { heading() }
    )

    Spacer(modifier = Modifier.height(10.56.rdp))

    Text(
        text = "Choose the amount of screen time you want to stay within each week.",
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 13.128.rsp,
        lineHeight = (13.128 * GeistLineHeight).rsp,
        letterSpacing = (-0.3938).sp,
        color = colorResource(R.color.my_promise_subtitle),
        style = FullLineBox,
        modifier = Modifier.padding(horizontal = ContentMargin)
    )
}

// ── Promise card ──────────────────────────────────────────────────

@Composable
private fun PromiseCard(dailyHours: Int, onDailyHoursChange: (Int) -> Unit) {
    // Set from the tap itself so both rolling numbers agree on direction.
    var rollUp by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .v3GradientCard()
            .padding(top = 25.5.rdp, bottom = 22.6.rdp)
    ) {
        Row(
            modifier = Modifier.padding(start = 28.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardLabel("SCREEN TIME")
            V3BulletDot(slotWidth = 19.rdp, dotSize = 3.5.rdp, color = Color.Black)
            CardLabel("PER WEEK")
        }

        Spacer(modifier = Modifier.height(23.9.rdp))

        WeeklyHoursStepper(
            dailyHours = dailyHours,
            rollUp = rollUp,
            onStep = { delta ->
                rollUp = delta > 0
                onDailyHoursChange(dailyHours + delta)
            }
        )

        Spacer(modifier = Modifier.height(12.94.rdp))

        DailyEquivalentLine(dailyHours = dailyHours, rollUp = rollUp)

        Spacer(modifier = Modifier.height(16.5.rdp))

        V3CardDivider()

        Spacer(modifier = Modifier.height(12.rdp))

        Text(
            text = "You can change your promise once a week. Your existing gold stays exactly where it is.",
            fontFamily = Geist,
            fontSize = 12.rsp,
            lineHeight = 18.72.rsp,
            letterSpacing = (-0.36).sp,
            color = Color.Black,
            style = FullLineBox,
            modifier = Modifier.padding(start = 27.rdp, end = 28.rdp)
        )
    }
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text = text,
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 12.rsp,
        lineHeight = (12 * GeistLineHeight).rsp,
        letterSpacing = (-0.36).sp,
        color = Color.Black,
        maxLines = 1,
        style = FullLineBox
    )
}

@Composable
private fun WeeklyHoursStepper(dailyHours: Int, rollUp: Boolean, onStep: (Int) -> Unit) {
    val weeklyHours = dailyHours * AppConstants.PROMISE_DAYS_PER_WEEK
    V3ValueStepper(
        value = weeklyHours,
        maxValue = AppConstants.PROMISE_MAX_DAILY_HOURS * AppConstants.PROMISE_DAYS_PER_WEEK,
        rollUp = rollUp,
        caption = "HRS/WEEK",
        captionLetterSpacing = 0.5362.sp,
        captionGap = 6.rdp,
        valueDescription = "$weeklyHours hours per week",
        decreaseLabel = "Decrease weekly promise",
        increaseLabel = "Increase weekly promise",
        canDecrease = dailyHours > AppConstants.PROMISE_MIN_DAILY_HOURS,
        canIncrease = dailyHours < AppConstants.PROMISE_MAX_DAILY_HOURS,
        onStep = onStep
    )
}

@Composable
private fun DailyEquivalentLine(dailyHours: Int, rollUp: Boolean) {
    val unit = if (dailyHours == 1) "hour" else "hours"
    val base = FullLineBox.copy(
        fontSize = 15.rsp,
        lineHeight = (15 * GeistLineHeight).rsp,
        letterSpacing = (-0.45).sp,
        color = BrandGreen
    )
    val mono = base.copy(fontFamily = DepartureMono)
    val bold = base.copy(fontFamily = Geist, fontWeight = FontWeight.Bold)
    val regular = base.copy(fontFamily = Geist, fontWeight = FontWeight.Normal)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "About $dailyHours $unit a day, your daily promise" },
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "≈ ", style = mono, maxLines = 1, modifier = Modifier.alignByBaseline())
        // The rolling slots move during the animation, so their live baseline isn't stable;
        // Geist's line box equals its font height here, so the baseline is its ascent ratio.
        V3RollingNumber(
            value = dailyHours,
            maxValue = AppConstants.PROMISE_MAX_DAILY_HOURS,
            rollUp = rollUp,
            style = bold,
            modifier = Modifier.alignBy { (it.measuredHeight * GeistAscentRatio).roundToInt() }
        )
        Text(text = " $unit a day ", style = bold, maxLines = 1, modifier = Modifier.alignByBaseline())
        Text(text = "·", style = mono, maxLines = 1, modifier = Modifier.alignByBaseline())
        Text(text = " your daily promise", style = regular, maxLines = 1, modifier = Modifier.alignByBaseline())
    }
}

// ── Rules note ────────────────────────────────────────────────────

@Composable
private fun PromiseRulesNote(dailyHours: Int) {
    val green = BrandGreen
    val hrs = if (dailyHours == 1) "hr" else "hrs"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.rdp))
            .background(colorResource(R.color.my_promise_note_bg))
            .padding(horizontal = 11.5.rdp, vertical = 18.rdp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(R.drawable.ic_my_promise_circle_check),
            contentDescription = null,
            modifier = Modifier.size(15.511.rdp)
        )
        Spacer(modifier = Modifier.width(4.489.rdp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                    append("Keep your screen time under")
                }
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = green)) {
                    append(
                        "$dailyHours $hrs on ${AppConstants.PROMISE_DAYS_TO_UNLOCK} of " +
                            "${AppConstants.PROMISE_DAYS_PER_WEEK} days"
                    )
                }
                withStyle(SpanStyle(color = green)) { append(",") }
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                    append(
                        "and Invest unlocks for the week. Miss your promise? Nothing is taken away. " +
                            "Invest simply stays locked. Monday starts a new week."
                    )
                }
            },
            style = FullLineBox.copy(
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 13.573.rsp,
                lineHeight = 19.0.rsp,
                color = colorResource(R.color.my_promise_note_text)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Footer ────────────────────────────────────────────────────────

@Composable
private fun BrandStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(53.rdp)
            .clipToBounds()
    ) {
        Image(
            painter = painterResource(R.drawable.bg_my_promise_pattern_strip),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .requiredSize(width = 413.007.rdp, height = 53.rdp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clearAndSetSemantics { contentDescription = "ZenMode OS" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_zen_mark_gradient),
                contentDescription = null,
                modifier = Modifier.size(20.556.rdp)
            )
            Spacer(modifier = Modifier.width(3.28.rdp))
            Text(
                text = "ZenMode",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 17.944.rsp,
                lineHeight = (17.944 * ClashLineHeight).rsp,
                letterSpacing = (-1.0766).sp,
                color = colorResource(R.color.my_promise_logo_text),
                maxLines = 1,
                style = FullLineBox
            )
            Text(
                text = "OS",
                fontFamily = ClashDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 17.944.rsp,
                lineHeight = (17.944 * ClashLineHeight).rsp,
                maxLines = 1,
                style = FullLineBox.copy(brush = rememberBrandOsGradient())
            )
        }
    }
}

/**
 * Figma's "Branf zen gradient" style: a CSS linear-gradient at -67.92deg through
 * score_grad_start / score_grad_mid / score_orange. Brush.linearGradient can't express
 * a CSS angle against an unknown text box, so the line is resolved from the laid-out size.
 */
@Composable
private fun rememberBrandOsGradient(): Brush {
    val start = colorResource(R.color.score_grad_start)
    val mid = colorResource(R.color.score_grad_mid)
    val end = colorResource(R.color.score_orange)
    return remember(start, mid, end) {
        cssLinearGradient(
            angleDegrees = -67.92291865051479f,
            colors = listOf(start, mid, end),
            stops = listOf(0.23558f, 0.50636f, 0.71412f)
        )
    }
}

private fun cssLinearGradient(angleDegrees: Float, colors: List<Color>, stops: List<Float>): ShaderBrush =
    object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val radians = Math.toRadians(angleDegrees.toDouble())
            val dx = sin(radians).toFloat()
            val dy = -cos(radians).toFloat()
            val halfLength = (abs(size.width * dx) + abs(size.height * dy)) / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            return LinearGradientShader(
                from = Offset(cx - dx * halfLength, cy - dy * halfLength),
                to = Offset(cx + dx * halfLength, cy + dy * halfLength),
                colors = colors,
                colorStops = stops
            )
        }
    }

@Composable
private fun OutlinedPillButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.rdp)
            .clip(shape)
            .background(colorResource(R.color.my_promise_secondary_button_bg))
            .border(1.dp, BrandGreen, shape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        V3PillButtonText(text = text, color = colorResource(R.color.stone_600))
    }
}
