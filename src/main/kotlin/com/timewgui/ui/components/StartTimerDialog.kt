package com.timewgui.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions

@Composable
fun StartTimerDialog(
    availableTags: List<String>,
    onStart: (tags: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTimewColors.current
    var selectedTags by remember { mutableStateOf(listOf<String>()) }
    var backdatedTime by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bgPrimary,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(TimewDimensions.borderRadiusCard),
        modifier = Modifier
            .border(1.dp, colors.border, RoundedCornerShape(TimewDimensions.borderRadiusCard)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Timer",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TagSelector(
                    selectedTags = selectedTags,
                    availableTags = availableTags,
                    onTagAdded = { selectedTags = selectedTags + it },
                    onTagRemoved = { selectedTags = selectedTags - it }
                )

                OutlinedTextField(
                    value = backdatedTime,
                    onValueChange = { backdatedTime = it },
                    label = { Text("Start time (optional)", color = colors.textSecondary) },
                    placeholder = { Text("HH:mm", color = colors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        cursorColor = colors.accent,
                        focusedLabelColor = colors.accent
                    )
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.bgTertiary),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null
                ) {
                    Text("Cancel", color = colors.textPrimary)
                }
                Button(
                    onClick = {
                        onStart(selectedTags)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null
                ) {
                    Text("Start", color = Color.White)
                }
            }
        },
        dismissButton = null
    )
}
