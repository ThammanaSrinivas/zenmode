package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zenlauncher.zenmode.AppConstants.PRODUCT_NAME
import com.zenlauncher.zenmode.R
import androidx.compose.ui.res.stringResource

@Composable
fun ForceUpdateDialog(
    onUpdateClick: () -> Unit,
    onTomorrowClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = stringResource(R.string.force_update_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.force_update_message, PRODUCT_NAME),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUpdateClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.force_update_now_button))
                }
                
                OutlinedButton(
                    onClick = onTomorrowClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.force_update_tomorrow_button))
                }
            }
        }
    )
}
