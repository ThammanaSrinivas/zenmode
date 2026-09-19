package com.zenlauncher.zenmode.ui.screens

import com.zenlauncher.zenmode.ui.components.BrandedText
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.ClashDisplay
import com.zenlauncher.zenmode.ui.theme.DepartureMono
import com.zenlauncher.zenmode.ui.theme.Geist
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp
import kotlinx.coroutines.launch

sealed class BuddyAddResult {
    data class Success(val buddyName: String) : BuddyAddResult()
    data class Error(val message: String) : BuddyAddResult()
    data class AlreadyBuddies(val buddyName: String) : BuddyAddResult()
    data object SelfAdd : BuddyAddResult()
}

// ── Zen Bro connect, screen 2 ─────────────────────────────────────
// "My Zen Circle" — reached from the home screen's "Add Buddy" (Figma node 2026:2443).
// Three ways to connect: share a link, trade codes, or random connect.
// Light-only like the other v3 sub-screens, so colors come straight from colors.xml.

private val CardRadius: Dp @Composable get() = 24.rdp
private val StepTextStart: Dp @Composable get() = 42.rdp
internal val PillHeight: Dp @Composable get() = 47.rdp

/**
 * Full-screen "My Zen Circle" page. [userCode] is the signed-in user's code (null when
 * signed out). [onAddBuddy] runs the code connect and reports back; everything else is
 * a fire-and-forget action owned by the host.
 */
@Composable
fun ZenBroConnectScreen(
    userCode: String?,
    onBackClick: () -> Unit,
    onShareLink: () -> Unit,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult,
    onRandomConnect: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val washEdge = colorResource(R.color.wash_neutral_edge)
    val washCore = colorResource(R.color.wash_neutral_core)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(washEdge, washCore, washEdge)))
            // Swallow touches so the home screen underneath never receives them.
            .pointerInput(Unit) { detectTapGestures() }
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.rdp)
        ) {
            Spacer(Modifier.height(33.rdp))
            ZenCircleHeader(onBackClick = onBackClick)

            Spacer(Modifier.height(39.rdp))
            Column(modifier = Modifier.padding(horizontal = 32.rdp)) {
                Text(
                    text = "Connect with your Zen Bro",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.rsp,
                    lineHeight = 42.rsp,
                    letterSpacing = (-0.56).sp,
                    color = Color.Black,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Choose how you want to connect",
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.rsp,
                    lineHeight = 27.rsp,
                    letterSpacing = (-0.36).sp,
                    color = colorResource(R.color.zen_circle_subtitle)
                )
            }

            Spacer(Modifier.height(23.rdp))
            ZenCircleConnectOptions(
                userCode = userCode,
                onShareLink = onShareLink,
                onCopyCode = onCopyCode,
                onAddBuddy = onAddBuddy,
                onRandomConnect = onRandomConnect,
                modifier = Modifier.padding(horizontal = 29.5.rdp)
            )
        }
    }
}

/** The three ways into a Zen Circle — share a link, trade codes, random connect. Also used by onboarding. */
@Composable
internal fun ZenCircleConnectOptions(
    userCode: String?,
    onShareLink: () -> Unit,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult,
    onRandomConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.rdp)
    ) {
        ShareLinkCard(enabled = userCode != null, onShareLink = onShareLink)
        UseCodeCard(userCode = userCode, onCopyCode = onCopyCode, onAddBuddy = onAddBuddy)
        RandomConnectCard(onRandomConnect = onRandomConnect)
    }
}

// ── Header ────────────────────────────────────────────────────────

/** Back arrow, "My Zen Circle" wordmark with its Beta tag, and an optional ☰ settings menu. */
@Composable
internal fun ZenCircleHeader(onBackClick: () -> Unit, onMenuClick: (() -> Unit)? = null) {
    val brandGreen = colorResource(R.color.gold_delta_text)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(37.rdp)
    ) {
        if (onMenuClick != null) {
            MenuButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = -(32.3.rdp - (48.dp - 29.3.rdp) / 2))
            )
        }
        // 48dp hit area centred on the 29.6dp arrow.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 32.rdp - (48.dp - 29.6.rdp) / 2)
                .requiredSize(48.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = "Back to home", onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_zen_circle_back),
                contentDescription = "Back",
                modifier = Modifier
                    .width(29.6.rdp)
                    .height(20.3.rdp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.rdp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_zen_circle_mark),
                contentDescription = null,
                modifier = Modifier
                    .width(28.7.rdp)
                    .height(28.2.rdp)
            )
            Box {
                Text(
                    text = "My Zen Circle",
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 30.2.rsp,
                    letterSpacing = (-2.4).sp,
                    color = colorResource(R.color.zen_900),
                    maxLines = 1
                )
                // "Beta" tag hangs off the title's top-right corner (node 2026:2453).
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 36.rdp, y = 3.rdp)
                        .width(31.2.rdp)
                        .height(13.1.rdp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, brandGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Beta",
                        fontFamily = Geist,
                        fontSize = 7.rsp,
                        lineHeight = 7.rsp,
                        letterSpacing = (-0.14).sp,
                        color = brandGreen,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Figma node 2026:2942. On tap the three lines squeeze together and spring back apart. */
@Composable
private fun MenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val squeeze = remember { Animatable(1f) }
    Box(
        modifier = modifier
            .requiredSize(48.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = "Zen Circle settings") {
                scope.launch {
                    squeeze.animateTo(0.45f, tween(90))
                    squeeze.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_zen_circle_menu),
            contentDescription = "Menu",
            modifier = Modifier
                .width(29.3.rdp)
                .height(16.68.rdp)
                .graphicsLayer { scaleY = squeeze.value }
        )
    }
}

// ── Cards ─────────────────────────────────────────────────────────

@Composable
private fun ShareLinkCard(enabled: Boolean, onShareLink: () -> Unit) {
    val brandGreen = colorResource(R.color.gold_delta_text)

    // The recommended option carries the green top rule (node 2026:2459).
    StepCard(number = "01", title = "Share a link", topAccent = brandGreen, trailing = {
        Box(
            modifier = Modifier
                .width(70.rdp)
                .height(20.rdp)
                .clip(RoundedCornerShape(4.rdp))
                .background(brandGreen.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Easiest",
                fontFamily = DepartureMono,
                fontSize = 13.rsp,
                lineHeight = 13.rsp,
                letterSpacing = (-1.17).sp,
                color = colorResource(R.color.zen_circle_chip_text),
                maxLines = 1
            )
        }
    }) {
        StepBody("Anyone can join even without having ZenMode OS installed.")
        Spacer(Modifier.height(18.rdp))
        ZenCirclePillButton(
            text = "Share link",
            onClick = onShareLink,
            enabled = enabled,
            container = colorResource(R.color.zen_700),
            content = Color.White,
            modifier = Modifier.padding(horizontal = 12.rdp)
        )
    }
}

@Composable
private fun UseCodeCard(
    userCode: String?,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var buddyCode by rememberSaveable { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val canConnect = buddyCode.isNotBlank() && !isConnecting
    val connect: () -> Unit = connect@{
        if (!canConnect) return@connect
        focusManager.clearFocus()
        scope.launch {
            isConnecting = true
            status = null
            status = when (val result = onAddBuddy(buddyCode.trim())) {
                is BuddyAddResult.Success -> "Connected with ${result.buddyName}!" to true
                is BuddyAddResult.AlreadyBuddies -> "You're already connected with ${result.buddyName}." to false
                is BuddyAddResult.SelfAdd -> "That's your own code — paste your Zen Bro's." to false
                is BuddyAddResult.Error -> result.message to false
            }
            isConnecting = false
        }
    }

    StepCard(number = "02", title = "Use a code") {
        StepBody("Paste theirs or share yours.")
        Spacer(Modifier.height(11.rdp))

        Column(modifier = Modifier.padding(start = StepTextStart, end = 15.rdp)) {
            Text(
                text = "PASTE ZEN-CODE",
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 12.rsp,
                lineHeight = 15.6.rsp,
                letterSpacing = (-0.13).sp,
                color = colorResource(R.color.zen_circle_body)
            )
            Spacer(Modifier.height(5.rdp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.rdp)
            ) {
                CodeInput(
                    value = buddyCode,
                    onValueChange = {
                        buddyCode = it
                        status = null
                    },
                    onDone = connect,
                    modifier = Modifier.weight(162f)
                )
                ZenCirclePillButton(
                    text = if (isConnecting) "Connecting…" else "Connect",
                    onClick = connect,
                    enabled = canConnect,
                    // Figma shows Connect solid even before a code is typed.
                    dimWhenDisabled = false,
                    container = colorResource(R.color.ink_base),
                    content = Color.White,
                    modifier = Modifier.weight(129f)
                )
            }

            status?.let { (message, success) ->
                Spacer(Modifier.height(8.rdp))
                Text(
                    text = message,
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.rsp,
                    lineHeight = 17.rsp,
                    color = colorResource(if (success) R.color.gold_delta_text else R.color.ember_700)
                )
            }

            Spacer(Modifier.height(20.rdp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorResource(R.color.zen_circle_divider))
            )

            MyCodeRow(userCode = userCode, onCopyCode = onCopyCode)
        }
    }
}

@Composable
private fun RandomConnectCard(onRandomConnect: () -> Unit) {
    val brandGreen = colorResource(R.color.gold_delta_text)

    StepCard(number = "03", title = "Random connect") {
        StepBody("Meet someone building healthy phone habits.")
        Spacer(Modifier.height(12.rdp))
        ZenCirclePillButton(
            text = "Find a buddy",
            onClick = onRandomConnect,
            container = Color.Transparent,
            content = brandGreen,
            border = BorderStroke(1.dp, brandGreen),
            modifier = Modifier.padding(horizontal = 12.rdp)
        )
    }
}

// ── Building blocks ───────────────────────────────────────────────

/**
 * CSS-style top-only border on a rounded box: the outer rounded rect minus the box inset by
 * [width] at the top, so the rule tapers down into the corners like Figma's `border-t`.
 */
internal fun Modifier.topAccentBorder(color: Color, width: Dp, cornerRadius: Dp): Modifier = drawWithCache {
    val r = cornerRadius.toPx()
    val b = width.toPx()
    val outer = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
    }
    val innerTop = CornerRadius(r, (r - b).coerceAtLeast(0f))
    val inner = Path().apply {
        addRoundRect(
            RoundRect(
                left = 0f, top = b, right = size.width, bottom = size.height,
                topLeftCornerRadius = innerTop,
                topRightCornerRadius = innerTop,
                bottomRightCornerRadius = CornerRadius(r),
                bottomLeftCornerRadius = CornerRadius(r)
            )
        )
    }
    val border = Path.combine(PathOperation.Difference, outer, inner)
    onDrawBehind { drawPath(border, color) }
}

/**
 * White rounded card with a mono step number in the left gutter and the title at
 * [StepTextStart]. [topAccent] draws Figma's top-only border that tapers into the corners.
 */
@Composable
private fun StepCard(
    number: String,
    title: String,
    topAccent: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val radius = CardRadius
    val accentWidth = 2.rdp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(Color.White)
            .then(if (topAccent != null) Modifier.topAccentBorder(topAccent, accentWidth, radius) else Modifier)
            .padding(top = 21.rdp, bottom = 18.rdp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number,
                fontFamily = DepartureMono,
                fontSize = 16.rsp,
                letterSpacing = (-0.18).sp,
                color = colorResource(R.color.zen_circle_step_number),
                modifier = Modifier
                    .padding(start = 10.rdp)
                    .width(StepTextStart - 10.rdp)
            )
            Text(
                text = title,
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 20.rsp,
                lineHeight = 30.rsp,
                letterSpacing = (-0.22).sp,
                color = Color.Black,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (trailing != null) {
                trailing()
                Spacer(Modifier.width(40.rdp))
            }
        }
        Spacer(Modifier.height(3.rdp))
        content()
    }
}

@Composable
private fun StepBody(text: String) {
    BrandedText(
        text = text,
        style = TextStyle(
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 16.rsp,
            lineHeight = 20.8.rsp,
            letterSpacing = (-0.18).sp,
            color = colorResource(R.color.zen_circle_body)
        ),
        modifier = Modifier.padding(start = StepTextStart, end = 29.rdp)
    )
}

@Composable
internal fun ZenCirclePillButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dimWhenDisabled: Boolean = true,
    border: BorderStroke? = null,
    height: Dp = PillHeight,
    fontSize: TextUnit = 16.rsp,
    letterSpacing: TextUnit = (-0.32).sp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled || !dimWhenDisabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(container)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun CodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val codeStyle = monoCodeStyle(15.rsp, colorResource(R.color.zen_circle_code_text))
    val hintColor = colorResource(R.color.zen_circle_input_hint)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = codeStyle,
        cursorBrush = SolidColor(colorResource(R.color.gold_delta_text)),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .height(PillHeight)
            .clip(RoundedCornerShape(8.rdp))
            .background(colorResource(R.color.zen_circle_input_bg))
            .border(1.dp, colorResource(R.color.zen_circle_input_border), RoundedCornerShape(8.rdp)),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.padding(horizontal = 14.rdp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    // Codes are long (raw UIDs) and can't be retyped from memory, so the hint
                    // points at pasting rather than implying a short fixed-format code.
                    Text(text = "Paste code here", style = codeStyle.copy(color = hintColor), maxLines = 1)
                }
                inner()
            }
        }
    )
}

@Composable
private fun MyCodeRow(userCode: String?, onCopyCode: () -> Unit) {
    val brandGreen = colorResource(R.color.gold_delta_text)
    val canCopy = userCode != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.rdp))
            .clickable(enabled = canCopy, onClickLabel = "Copy my code", onClick = onCopyCode)
            .padding(top = 4.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            // Codes are long; the row shows a prefix and "Copy my code" copies all of it.
            text = userCode?.let { if (it.length > 8) "${it.take(8)}…" else it } ?: "Sign in first",
            style = monoCodeStyle(16.rsp, colorResource(R.color.zen_circle_code_text)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Image(
            painter = painterResource(R.drawable.ic_zen_circle_copy),
            contentDescription = null,
            modifier = Modifier
                .offset(x = (-4).rdp)
                .size(31.rdp)
                .alpha(if (canCopy) 1f else 0.4f)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Copy my code",
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 16.rsp,
            letterSpacing = (-0.32).sp,
            color = brandGreen.copy(alpha = if (canCopy) 1f else 0.4f),
            maxLines = 1
        )
    }
}

@Composable
private fun monoCodeStyle(size: TextUnit, color: Color) = TextStyle(
    fontFamily = DepartureMono,
    fontSize = size,
    letterSpacing = (-0.17).sp,
    color = color
)
