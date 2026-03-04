package com.timewgui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Box(
        modifier = Modifier.fillMaxSize().background(colors.bgPrimary).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardSurface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Idle Detected",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textOnCardPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "You\u2019ve been idle for $idleDurationMinutes minute${if (idleDurationMinutes != 1L) "s" else ""}.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textOnCardPrimary
            )
            Text(
                text = "What would you like to do with the tracked time?",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textOnCardSecondary
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onStopTimer,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.destructive)
                ) {
                    Text("Stop Timer")
                }
                OutlinedButton(
                    onClick = onPauseAndResume,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                ) {
                    Text("Pause & Resume")
                }
                Spacer(Modifier.width(4.dp))
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
    }
}
