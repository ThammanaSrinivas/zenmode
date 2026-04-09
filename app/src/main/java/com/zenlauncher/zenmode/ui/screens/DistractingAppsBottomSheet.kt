package com.zenlauncher.zenmode.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.zenlauncher.zenmode.DistractingAppsRepository
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.WeightSpacer
import com.zenlauncher.zenmode.ui.theme.CabinetGrotesque
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import com.zenlauncher.zenmode.ui.theme.rdp
import com.zenlauncher.zenmode.ui.theme.rsp

data class DistractingAppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val isForced: Boolean,
    val isSelected: Boolean
)

/**
 * Loads launcher-visible installed apps and tags each as forced/selected based
 * on [DistractingAppsRepository]. Sorts forced apps first so the mandatory ones
 * render at the top of the grid.
 */
fun loadDistractingAppItems(context: Context): List<DistractingAppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val activities = pm.queryIntentActivities(intent, 0)
    val userSelected = DistractingAppsRepository.getUserSelected(context)
    val ownPackage = context.packageName

    return activities
        .map { it.activityInfo.packageName to it }
        .distinctBy { it.first }
        .filter { it.first != ownPackage }
        .map { (pkg, resolveInfo) ->
            val forced = DistractingAppsRepository.isForced(pm, pkg)
            DistractingAppItem(
                label = resolveInfo.loadLabel(pm).toString(),
                packageName = pkg,
                icon = resolveInfo.loadIcon(pm),
                isForced = forced,
                isSelected = forced || pkg in userSelected
            )
        }
        .sortedWith(compareByDescending<DistractingAppItem> { it.isForced }.thenBy { it.label.lowercase() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistractingAppsBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = ZenTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var apps by remember { mutableStateOf<List<DistractingAppItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        apps = loadDistractingAppItems(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgSecondary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.rdp, bottom = 8.rdp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textSecondary)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.rdp)
                .padding(bottom = 24.rdp)
        ) {
            // Header row: title + subtitle on the left, app icon on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Change Distracting app list",
                        color = colors.textPrimary,
                        style = TextStyle(
                            fontFamily = CabinetGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.rsp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pick the apps dragging you from zen.",
                        color = colors.textSecondary,
                        style = TextStyle(
                            fontFamily = CabinetGrotesque,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.rsp
                        )
                    )

                    Spacer(modifier = Modifier.height(70.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    DistractingAppCell(
                        app = app,
                        onClick = {
                            if (!app.isForced) {
                                DistractingAppsRepository.toggleUserSelected(context, app.packageName)
                                apps = loadDistractingAppItems(context)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DistractingAppCell(app: DistractingAppItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clickable(enabled = !app.isForced, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = remember(app.packageName) {
            app.icon.toBitmap(width = 128, height = 128).asImageBitmap()
        }
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .alpha(if (app.isSelected) 1f else 0.55f)
        )
        if (app.isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00C700)),
                contentAlignment = Alignment.Center
            ) {
                CheckMark()
            }
        }
    }
}

@Composable
private fun CheckMark() {
    Canvas(modifier = Modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = Color.White,
            start = Offset(w * 0.15f, h * 0.55f),
            end = Offset(w * 0.42f, h * 0.80f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.42f, h * 0.80f),
            end = Offset(w * 0.85f, h * 0.25f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

