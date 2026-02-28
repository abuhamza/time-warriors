package com.timewgui.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timewgui.ui.theme.LocalTimewColors

@Composable
fun IdleDialog(
    idleDurationMinutes: Long,
    onKeepTracking: () -> Unit,
    onPauseAndResume: () -> Unit,
    onStopTimer: () -> Unit
) {
    val colors = LocalTimewColors.current

    AlertDialog(
        onDismissRequest = onKeepTracking,
        containerColor = colors.cardSurface,
        titleContentColor = colors.textOnCardPrimary,
        textContentColor = colors.textOnCardSecondary,
        title = {
            Text(
                text = "Idle Detected",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "You\u2019ve been idle for $idleDurationMinutes minute${if (idleDurationMinutes != 1L) "s" else ""}.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "What would you like to do with the tracked time?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onStopTimer,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.destructive
                    )
                ) {
                    Text("Stop Timer")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onPauseAndResume,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.accent
                    )
                ) {
                    Text("Pause & Resume")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onKeepTracking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.success,
                        contentColor = colors.bgPrimary
                    )
                ) {
                    Text("Keep Tracking")
                }
            }
        }
    )
}
