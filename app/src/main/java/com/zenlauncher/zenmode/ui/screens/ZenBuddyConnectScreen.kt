package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.RedditMono
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.launch

sealed class BuddyAddResult {
    data class Success(val buddyName: String) : BuddyAddResult()
    data class Error(val message: String) : BuddyAddResult()
    data class AlreadyBuddies(val buddyName: String) : BuddyAddResult()
    data object SelfAdd : BuddyAddResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenBuddyConnectBottomSheet(
    userCode: String?,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult,
    onRandomConnect: () -> Unit,
    onWatchVideo: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ZenTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textSecondary)
            )
        }
    ) {
        ZenBuddyConnectContent(
            userCode = userCode,
            onCopyCode = onCopyCode,
            onAddBuddy = onAddBuddy,
            onRandomConnect = onRandomConnect,
            onWatchVideo = onWatchVideo
        )
    }
}

@Composable
private fun ZenBuddyConnectContent(
    userCode: String?,
    onCopyCode: () -> Unit,
    onAddBuddy: suspend (String) -> BuddyAddResult,
    onRandomConnect: () -> Unit,
    onWatchVideo: () -> Unit
) {
    val colors = ZenTheme.colors
    val scope = rememberCoroutineScope()

    var buddyCode by remember { mutableStateOf("") }
    var isAddingBuddy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val displayCode = when {
        userCode == null -> "Not Signed In"
        userCode.length > 7 -> "${userCode.take(7)}..."
        else -> userCode
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Green glow effect in top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(150.dp)
                .offset(x = 40.dp, y = (-40).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.borderFocus.copy(alpha = 0.35f),
                            colors.borderFocus.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Title Row ──
            TitleRow(displayCode = displayCode, onCopyCode = onCopyCode)

            Spacer(Modifier.height(28.dp))

            // ── Section 1: Add Zen Buddy ──
            AddBuddySection(
                buddyCode = buddyCode,
                onBuddyCodeChange = { buddyCode = it },
                displayCode = displayCode,
                statusMessage = statusMessage,
                isSuccess = isSuccess,
                isAddingBuddy = isAddingBuddy,
                onAddBuddy = {
                    val trimmedCode = buddyCode.trim()
                    if (trimmedCode == userCode) {
                        isSuccess = false
                        statusMessage = "You cannot add yourself as a buddy!"
                    } else scope.launch {
                        isAddingBuddy = true
                        statusMessage = null
                        val result = onAddBuddy(trimmedCode)
                        isAddingBuddy = false
                        when (result) {
                            is BuddyAddResult.Success -> {
                                isSuccess = true
                                statusMessage = "Successfully added ${result.buddyName}!"
                            }
                            is BuddyAddResult.Error -> {
                                isSuccess = false
                                statusMessage = result.message
                            }
                            is BuddyAddResult.AlreadyBuddies -> {
                                isSuccess = false
                                statusMessage = "You are already buddies with ${result.buddyName}!"
                            }
                            is BuddyAddResult.SelfAdd -> {
                                isSuccess = false
                                statusMessage = "You cannot add yourself as a buddy."
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // ── Divider ──
            HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)

            Spacer(Modifier.height(24.dp))

            // ── Section 2: Random Connect ──
            RandomConnectSection(onRandomConnect = onRandomConnect)

            Spacer(Modifier.height(28.dp))

            // ── Footer Video Link ──
            VideoLink(onWatchVideo = onWatchVideo)
        }
    }
}

// ── Title Row ────────────────────────────────────────────────────

@Composable
private fun TitleRow(displayCode: String, onCopyCode: () -> Unit) {
    val colors = ZenTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Title with heart
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zen Buddy Connect",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = colors.textPrimary
                )
                Spacer(Modifier.width(4.dp))
                Image(
                    painter = painterResource(R.drawable.heart_sharukhan),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // My code row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "My zenmode code: ",
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = displayCode,
                    fontFamily = RedditMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
                Spacer(Modifier.width(6.dp))
                Image(
                    painter = painterResource(R.drawable.ic_content_copy),
                    contentDescription = "Copy code",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCopyCode() },
                    colorFilter = ColorFilter.tint(colors.textSecondary)
                )
            }
        }

        // App icon top right
        Image(
            painter = painterResource(R.drawable.app_icon),
            contentDescription = "ZenMode",
            modifier = Modifier.size(44.dp)
        )
    }
}

// ── Section 1: Add Zen Buddy ─────────────────────────────────────

@Composable
private fun AddBuddySection(
    buddyCode: String,
    onBuddyCodeChange: (String) -> Unit,
    displayCode: String,
    statusMessage: String?,
    isSuccess: Boolean,
    isAddingBuddy: Boolean,
    onAddBuddy: () -> Unit
) {
    val colors = ZenTheme.colors
    val isButtonEnabled = buddyCode.isNotBlank() && !isAddingBuddy

    Text(
        text = "1. Single Forever? No Worries\uD83E\uDEE3",
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        color = colors.textPrimary
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = "ENTER YOUR BUDDY'S CODE:",
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = colors.textBrand
    )

    Spacer(Modifier.height(8.dp))

    // Input field
    BasicTextField(
        value = buddyCode,
        onValueChange = onBuddyCodeChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgSecondary)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        textStyle = TextStyle(
            fontFamily = CabinetGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colors.textPrimary
        ),
        singleLine = true,
        cursorBrush = SolidColor(colors.textBrand),
        decorationBox = { innerTextField ->
            if (buddyCode.isEmpty()) {
                Text(
                    text = displayCode,
                    fontFamily = CabinetGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textSecondary.copy(alpha = 0.5f)
                )
            }
            innerTextField()
        }
    )

    Spacer(Modifier.height(8.dp))

    // Status / description message
    Text(
        text = statusMessage
            ?: "Congrats for start accompanying your friend on mindful journey!!",
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = if (isSuccess) colors.textBrand else colors.textSecondary
    )

    Spacer(Modifier.height(16.dp))

    // Add zen buddy button
    Image(
        painter = painterResource(R.drawable.button_add_zen_buddy),
        contentDescription = "Add zen buddy",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .alpha(if (isButtonEnabled) 1f else 0.5f)
            .clickable(enabled = isButtonEnabled) { onAddBuddy() },
        contentScale = ContentScale.FillWidth
    )
}

// ── Section 2: Random Connect ────────────────────────────────────

@Composable
private fun RandomConnectSection(onRandomConnect: () -> Unit) {
    val colors = ZenTheme.colors

    Text(
        text = "2. Single Forever? No Worries\uD83E\uDEE3",
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        color = colors.textPrimary
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Our random connect, let\u2019s you connect with random people on their mindful journey!",
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = colors.textSecondary
    )

    Spacer(Modifier.height(16.dp))

    // Random connect button (outlined)
    Image(
        painter = painterResource(R.drawable.button_random_connect),
        contentDescription = "Random connect",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onRandomConnect() },
        contentScale = ContentScale.FillWidth
    )
}

// ── Footer Video Link ────────────────────────────────────────────

@Composable
private fun VideoLink(onWatchVideo: () -> Unit) {
    val colors = ZenTheme.colors

    Text(
        text = "Watch video on how add your friend as zen buddy(\uD83D\uDCFA)",
        fontFamily = CabinetGrotesque,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = colors.textBrand,
        modifier = Modifier.clickable { onWatchVideo() }
    )
}
