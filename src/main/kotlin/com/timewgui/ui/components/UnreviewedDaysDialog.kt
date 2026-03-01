package com.timewgui.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.timewgui.ui.theme.LocalTimewColors
import kotlinx.datetime.LocalDate

@Composable
fun UnreviewedDaysDialog(
    unreviewedDays: List<LocalDate>,
    onExcludeSelected: (selectedDates: List<LocalDate>, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTimewColors.current
    var selectedDates by remember { mutableStateOf(unreviewedDays.toSet()) }
    var label by remember { mutableStateOf("Vacation") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        titleContentColor = colors.textOnCardPrimary,
        textContentColor = colors.textOnCardSecondary,
        title = {
            Text(
                text = "Unreviewed Days",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "These workdays have 0 tracked hours. Select days to mark as excluded (vacation, holiday, etc.).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label", color = colors.textOnCardSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderOnCard,
                        cursorColor = colors.accent
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedDates.size} of ${unreviewedDays.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textOnCardSecondary
                    )
                    Row {
                        Text(
                            text = if (selectedDates.size == unreviewedDays.size) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                            modifier = Modifier.padding(end = 4.dp).let { mod ->
                                mod
                            }
                        )
                        Checkbox(
                            checked = selectedDates.size == unreviewedDays.size,
                            onCheckedChange = { selectAll ->
                                selectedDates = if (selectAll) unreviewedDays.toSet() else emptySet()
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.accent,
                                uncheckedColor = colors.textOnCardSecondary
                            )
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(unreviewedDays) { date ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = date in selectedDates,
                                onCheckedChange = { checked ->
                                    selectedDates = if (checked) selectedDates + date else selectedDates - date
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.accent,
                                    uncheckedColor = colors.textOnCardSecondary
                                )
                            )
                            Text(
                                text = "${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, $date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textOnCardPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.textOnCardSecondary
                    )
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (selectedDates.isNotEmpty()) {
                            onExcludeSelected(selectedDates.toList().sorted(), label)
                        }
                    },
                    enabled = selectedDates.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.bgPrimary
                    )
                ) {
                    Text("Exclude Selected")
                }
            }
        }
    )
}
