package com.zenlauncher.zenmode.ui.screens

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.zenlauncher.zenmode.RandomConnect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.MoodBackdrop
import com.zenlauncher.zenmode.ui.components.taperedBorder
import com.zenlauncher.zenmode.ui.theme.ZenTheme
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

/** Result of pasting a code into [UseCodeCard] while [ZenBroConnectScreen.circleMode] is on. */
/** The line under the code field. It is built inside a coroutine, where a resource
 *  cannot be resolved, so a resource id is carried and resolved where it is shown. */
private sealed interface ConnectStatus {
    val success: Boolean
    data class Copy(
        @StringRes val res: Int,
        override val success: Boolean = false,
        val args: List<Any> = emptyList()
    ) : ConnectStatus
    data class Plain(val message: String, override val success: Boolean = false) : ConnectStatus
}

sealed class CircleCodeResult {
    data object Success : CircleCodeResult()
    data object CircleFull : CircleCodeResult()
    data object AlreadyInCircle : CircleCodeResult()
    data class Error(@StringRes val messageRes: Int) : CircleCodeResult()
}

// ── Zen Bro connect, screen 2 ─────────────────────────────────────
// "My Zen Circle" — reached from the home screen's "Add Buddy" (Figma node 2026:2443).
// Three ways to connect: share a link, trade codes, or random connect.
// Light-only like the other v3 sub-screens, so colors come straight from colors.xml.

// One grid for all three option cards: the step number sits in a 16dp gutter, and every
// title, line of body copy, input and button starts on the same StepTextStart column and
// stops StepTextEnd short of the card edge.
private val CardRadius: Dp @Composable get() = 20.rdp
private val StepNumberStart: Dp @Composable get() = 16.rdp
private val StepTextStart: Dp @Composable get() = 46.rdp
private val StepTextEnd: Dp @Composable get() = 16.rdp
private val StepSectionGap: Dp @Composable get() = 14.rdp
private val OptionPillHeight: Dp @Composable get() = 40.rdp
private val OptionFontSize: TextUnit @Composable get() = 15.rsp
internal val PillHeight: Dp @Composable get() = 47.rdp

/**
 * Full-screen "My Zen Circle" page. [userCode] is the signed-in user's code (null when
 * signed out). [onAddBuddy] runs the code connect and reports back; everything else is
 * a fire-and-forget action owned by the host.
 */
@Composable
fun ZenBroConnectScreen(
    userCode: String?,
    // True once BuddyFlowPreferences has the user on Zen Circle -- Share/Use a code/Random
    // connect all create or join a real circle instead of a classic 1:1 buddy pair.
    circleMode: Boolean = false,
    onBackClick: () -> Unit,
    onShareLink: () -> Unit,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult,
    onJoinCircleCode: suspend (String) -> CircleCodeResult = { CircleCodeResult.Error(R.string.zen_bro_circle_join_not_available) },
    onRandomConnect: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Swallow touches so the home screen underneath never receives them.
            .pointerInput(Unit) { detectTapGestures() }
    ) {
        MoodBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.rdp)
        ) {
            Spacer(Modifier.height(33.rdp))
            ZenCircleHeader(onBackClick = onBackClick)

            Spacer(Modifier.height(28.rdp))
            Column(modifier = Modifier.padding(horizontal = 30.rdp)) {
                Text(
                    text = stringResource(R.string.zen_bro_connect_title),
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.rsp,
                    lineHeight = 30.rsp,
                    letterSpacing = (-0.48).sp,
                    color = ZenTheme.colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.zen_bro_connect_subtitle),
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.rsp,
                    lineHeight = 21.rsp,
                    letterSpacing = (-0.3).sp,
                    color = colorResource(R.color.zen_circle_subtitle),
                    modifier = Modifier.padding(top = 4.rdp)
                )
            }

            Spacer(Modifier.height(20.rdp))
            ZenCircleConnectOptions(
                userCode = userCode,
                circleMode = circleMode,
                onShareLink = onShareLink,
                onCopyCode = onCopyCode,
                onAddBuddy = onAddBuddy,
                onJoinCircleCode = onJoinCircleCode,
                onRandomConnect = onRandomConnect,
                modifier = Modifier.padding(horizontal = 30.rdp)
            )
        }
    }
}

/** The three ways into a Zen Circle — share a link, trade codes, random connect. Also used by
 * onboarding (always with [circleMode] false -- onboarding doesn't offer circle creation yet). */
@Composable
internal fun ZenCircleConnectOptions(
    userCode: String?,
    circleMode: Boolean = false,
    onShareLink: () -> Unit,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult,
    onJoinCircleCode: suspend (String) -> CircleCodeResult = { CircleCodeResult.Error(R.string.zen_bro_circle_join_not_available) },
    onRandomConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.rdp)
    ) {
        ShareLinkCard(enabled = userCode != null, onShareLink = onShareLink)
        UseCodeCard(
            userCode = userCode,
            circleMode = circleMode,
            onCopyCode = onCopyCode,
            onAddBuddy = onAddBuddy,
            onJoinCircleCode = onJoinCircleCode
        )
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
                .clickable(onClickLabel = stringResource(R.string.common_back_to_home), onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_zen_circle_back),
                contentDescription = stringResource(R.string.common_back),
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
                    text = stringResource(R.string.zen_bro_circle_wordmark),
                    fontFamily = ClashDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 30.2.rsp,
                    letterSpacing = (-2.4).sp,
                    color = ZenTheme.colors.textBrandStrong,
                    maxLines = 1
                )
                // The "Beta" tag hangs off the title's top-right corner (node 2026:2453).
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
                        text = stringResource(R.string.zen_bro_beta_badge),
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
            .clickable(onClickLabel = stringResource(R.string.common_zen_circle_settings)) {
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
            contentDescription = stringResource(R.string.zen_bro_menu_content_description),
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
    StepCard(number = "01", title = stringResource(R.string.zen_bro_share_a_link), topAccent = brandGreen, trailing = {
        Box(
            modifier = Modifier
                .height(18.rdp)
                .clip(RoundedCornerShape(4.rdp))
                .background(brandGreen.copy(alpha = 0.9f))
                .padding(horizontal = 8.rdp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.zen_bro_easiest_badge),
                fontFamily = DepartureMono,
                fontSize = 11.rsp,
                lineHeight = 11.rsp,
                letterSpacing = (-0.6).sp,
                color = colorResource(R.color.zen_circle_chip_text),
                maxLines = 1
            )
        }
    }) {
        StepBody(stringResource(R.string.zen_bro_share_link_body, PRODUCT_NAME))
        Spacer(Modifier.height(StepSectionGap))
        ZenCirclePillButton(
            text = stringResource(R.string.zen_bro_share_link_button),
            onClick = onShareLink,
            enabled = enabled,
            container = colorResource(R.color.zen_700),
            content = Color.White,
            height = OptionPillHeight,
            fontSize = OptionFontSize,
            modifier = Modifier.stepContentPadding()
        )
    }
}

@Composable
private fun UseCodeCard(
    userCode: String?,
    circleMode: Boolean = false,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult,
    onJoinCircleCode: suspend (String) -> CircleCodeResult = { CircleCodeResult.Error(R.string.zen_bro_circle_join_not_available) }
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var buddyCode by rememberSaveable { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<ConnectStatus?>(null) }

    val canConnect = buddyCode.isNotBlank() && !isConnecting
    val connect: () -> Unit = connect@{
        if (!canConnect) return@connect
        focusManager.clearFocus()
        scope.launch {
            isConnecting = true
            status = null
            status = if (circleMode) {
                when (val result = onJoinCircleCode(buddyCode.trim())) {
                    is CircleCodeResult.Success -> null // navigates to the circle dashboard, nothing to show inline
                    is CircleCodeResult.CircleFull -> ConnectStatus.Copy(R.string.zen_bro_circle_full_error)
                    is CircleCodeResult.AlreadyInCircle -> ConnectStatus.Copy(R.string.zen_bro_already_in_circle_error)
                    is CircleCodeResult.Error -> ConnectStatus.Copy(result.messageRes)
                }
            } else {
                when (val result = onAddBuddy(buddyCode.trim())) {
                    is BuddyAddResult.Success -> ConnectStatus.Copy(R.string.zen_bro_connected_with_buddy, success = true, args = listOf(result.buddyName))
                    is BuddyAddResult.AlreadyBuddies -> ConnectStatus.Copy(R.string.zen_bro_already_connected_error, args = listOf(result.buddyName))
                    is BuddyAddResult.SelfAdd -> ConnectStatus.Copy(R.string.zen_bro_self_add_error)
                    is BuddyAddResult.Error -> ConnectStatus.Plain(result.message)
                }
            }
            isConnecting = false
        }
    }

    StepCard(number = "02", title = stringResource(R.string.zen_bro_use_a_code)) {
        StepBody(stringResource(R.string.zen_bro_use_code_body))
        Spacer(Modifier.height(StepSectionGap))

        Column(modifier = Modifier.stepContentPadding()) {
            Text(
                text = stringResource(R.string.zen_bro_paste_zen_code_label),
                fontFamily = Geist,
                fontWeight = FontWeight.Medium,
                fontSize = 11.rsp,
                lineHeight = 14.rsp,
                letterSpacing = 0.2.sp,
                color = colorResource(R.color.zen_circle_body)
            )
            Spacer(Modifier.height(6.rdp))

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
                    text = if (isConnecting) stringResource(R.string.zen_bro_connecting_button) else stringResource(R.string.zen_bro_connect_button),
                    onClick = connect,
                    enabled = canConnect,
                    // Figma shows Connect solid even before a code is typed.
                    dimWhenDisabled = false,
                    container = colorResource(R.color.ink_base),
                    content = Color.White,
                    height = OptionPillHeight,
                    fontSize = OptionFontSize,
                    modifier = Modifier.weight(110f)
                )
            }

            status?.let { st ->
                val message = when (st) {
                    is ConnectStatus.Copy -> stringResource(st.res, *st.args.toTypedArray())
                    is ConnectStatus.Plain -> st.message
                }
                val success = st.success
                Spacer(Modifier.height(8.rdp))
                Text(
                    text = message,
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.rsp,
                    lineHeight = 16.rsp,
                    color = colorResource(if (success) R.color.gold_delta_text else R.color.ember_700)
                )
            }

            Spacer(Modifier.height(StepSectionGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorResource(R.color.zen_circle_divider))
            )
            Spacer(Modifier.height(4.rdp))

            MyCodeRow(userCode = userCode, onCopyCode = onCopyCode)
        }
    }
}

@Composable
private fun RandomConnectCard(onRandomConnect: () -> Unit) {
    val brandGreen = colorResource(R.color.gold_delta_text)

    StepCard(number = "03", title = RandomConnect.NAME) {
        StepBody(stringResource(R.string.zen_bro_random_connect_body))
        Spacer(Modifier.height(StepSectionGap))
        ZenCirclePillButton(
            text = stringResource(R.string.zen_bro_find_buddy_button),
            onClick = onRandomConnect,
            container = Color.Transparent,
            content = brandGreen,
            border = BorderStroke(1.dp, brandGreen),
            height = OptionPillHeight,
            fontSize = OptionFontSize,
            modifier = Modifier.stepContentPadding()
        )
    }
}

// ── Building blocks ───────────────────────────────────────────────

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
            .background(ZenTheme.colors.surfaceElevated)
            .then(if (topAccent != null) Modifier.taperedBorder(topAccent, radius, top = accentWidth) else Modifier)
            .padding(vertical = 16.rdp)
    ) {
        Row(
            modifier = Modifier.padding(end = StepTextEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = number,
                fontFamily = DepartureMono,
                fontSize = 13.rsp,
                letterSpacing = (-0.18).sp,
                color = colorResource(R.color.zen_circle_step_number),
                modifier = Modifier
                    .padding(start = StepNumberStart)
                    .width(StepTextStart - StepNumberStart)
            )
            Text(
                text = title,
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.rsp,
                lineHeight = 22.rsp,
                letterSpacing = (-0.34).sp,
                color = ZenTheme.colors.textPrimary,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (trailing != null) {
                Spacer(Modifier.width(8.rdp))
                trailing()
            }
        }
        Spacer(Modifier.height(4.rdp))
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
            fontSize = 14.rsp,
            lineHeight = 19.rsp,
            letterSpacing = (-0.14).sp,
            color = colorResource(R.color.zen_circle_body)
        ),
        modifier = Modifier.stepContentPadding()
    )
}

/** Puts card content on the shared text column (see [StepTextStart]). */
@Composable
private fun Modifier.stepContentPadding(): Modifier = padding(start = StepTextStart, end = StepTextEnd)

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
    val codeStyle = monoCodeStyle(13.rsp, colorResource(R.color.zen_circle_code_text))
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
            .height(OptionPillHeight)
            .clip(RoundedCornerShape(8.rdp))
            .background(colorResource(R.color.zen_circle_input_bg))
            .border(1.dp, colorResource(R.color.zen_circle_input_border), RoundedCornerShape(8.rdp)),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.padding(horizontal = 12.rdp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    // Codes are long (raw UIDs) and can't be retyped from memory, so the hint
                    // points at pasting rather than implying a short fixed-format code.
                    Text(text = stringResource(R.string.zen_bro_paste_code_hint), style = codeStyle.copy(color = hintColor), maxLines = 1)
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
            .clickable(enabled = canCopy, onClickLabel = stringResource(R.string.common_copy_my_code), onClick = onCopyCode)
            .padding(top = 4.rdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            // Codes are long; the row shows a prefix and "Copy my code" copies all of it.
            text = userCode?.let { if (it.length > 8) "${it.take(8)}…" else it } ?: stringResource(R.string.zen_bro_sign_in_first),
            style = monoCodeStyle(14.rsp, colorResource(R.color.zen_circle_code_text)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Image(
            painter = painterResource(R.drawable.ic_zen_circle_copy),
            contentDescription = null,
            modifier = Modifier
                .size(26.rdp)
                .alpha(if (canCopy) 1f else 0.4f)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.common_copy_my_code),
            fontFamily = Geist,
            fontWeight = FontWeight.Medium,
            fontSize = 14.rsp,
            letterSpacing = (-0.28).sp,
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
