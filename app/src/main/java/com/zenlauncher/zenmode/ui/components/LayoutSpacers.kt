package com.zenlauncher.zenmode.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Utility composable for creating proportional consistent vertical gaps.
 * This can be used to distribute space consistently across onboarding pages.
 */
@Composable
fun ColumnScope.WeightSpacer(weight: Float) {
    if (weight > 0f) {
        Spacer(modifier = Modifier.weight(weight))
    }
}

/**
 * Utility composable for creating proportional consistent horizontal gaps.
 */
@Composable
fun RowScope.WeightSpacer(weight: Float) {
    if (weight > 0f) {
        Spacer(modifier = Modifier.weight(weight))
    }
}
