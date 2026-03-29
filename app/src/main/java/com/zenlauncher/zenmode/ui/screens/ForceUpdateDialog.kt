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

@Composable
fun ForceUpdateDialog(
    onUpdateClick: () -> Unit,
    onExitClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = "Update Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "A new version of ZenMode is required to continue. Please update to the latest version to enjoy new features and improvements.",
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
                    Text("Update Now")
                }
                
                OutlinedButton(
                    onClick = onExitClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exit App")
                }
            }
        }
    )
}
