package com.timewgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timewgui.domain.model.ExcludedDateRange
import com.timewgui.domain.model.ExclusionSource
import com.timewgui.domain.system.LaunchAtLogin
import com.timewgui.ui.components.TagSelector
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.viewmodel.AppState
import com.timewgui.viewmodel.OvertimeViewModel
import com.timewgui.viewmodel.TagViewModel
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

@Composable
fun SettingsScreen(
    appState: AppState,
    tagViewModel: TagViewModel,
    overtimeViewModel: OvertimeViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    val themePreference = when (appState.isDarkTheme) {
        null -> ThemePreference.SYSTEM
        false -> ThemePreference.LIGHT
        true -> ThemePreference.DARK
    }
    val isMacOS = remember { System.getProperty("os.name").lowercase().contains("mac") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgPrimary)
            .padding(TimewDimensions.sectionGap)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TimewDimensions.sectionGap)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        // Theme card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemePreference.entries.forEach { pref ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = themePreference == pref,
                            onClick = {
                                when (pref) {
                                    ThemePreference.SYSTEM -> appState.updateDarkTheme(null)
                                    ThemePreference.LIGHT -> appState.setDarkThemeOverride(false)
                                    ThemePreference.DARK -> appState.setDarkThemeOverride(true)
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.accent,
                                unselectedColor = colors.textOnCardSecondary
                            )
                        )
                        Text(
                            text = pref.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textOnCardPrimary
                        )
                    }
                }
            }
        }

        // Idle Detection card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "Idle Detection",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Detect when you\u2019re away",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textOnCardPrimary
                    )
                    Text(
                        text = "Shows a dialog when no input is detected while the timer is running",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textOnCardSecondary
                    )
                }
                Switch(
                    checked = appState.idleDetectionEnabled,
                    onCheckedChange = { appState.updateIdleDetectionEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.cardSurface,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textOnCardTertiary,
                        uncheckedTrackColor = colors.borderOnCard
                    )
                )
            }

            if (appState.idleDetectionEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Threshold",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textOnCardPrimary
                    )
                    var thresholdText by remember(appState.idleThresholdMinutes) {
                        mutableStateOf(appState.idleThresholdMinutes.toString())
                    }
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { value ->
                            thresholdText = value.filter { it.isDigit() }
                            thresholdText.toIntOrNull()?.let { appState.updateIdleThresholdMinutes(it) }
                        },
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderOnCard,
                            cursorColor = colors.accent
                        )
                    )
                    Text(
                        text = "minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textOnCardSecondary
                    )
                }
            }
        }

        // Launch at Login card (macOS only)
        if (isMacOS) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                    .background(colors.cardSurface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Launch at Login",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textOnCardPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textOnCardPrimary
                        )
                        Text(
                            text = "Open TimewGUI when you log in to your Mac",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textOnCardSecondary
                        )
                    }
                    Switch(
                        checked = appState.launchAtLogin,
                        onCheckedChange = { enabled ->
                            appState.updateLaunchAtLogin(enabled)
                            if (enabled) LaunchAtLogin.enable() else LaunchAtLogin.disable()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.cardSurface,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.textOnCardTertiary,
                            uncheckedTrackColor = colors.borderOnCard
                        )
                    )
                }
            }
        }

        // Default Context Tags card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "Default Context Tags",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Tags automatically added to new tasks (e.g. \"work\")",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnCardSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            TagSelector(
                selectedTags = appState.defaultContextTags,
                availableTags = tagViewModel.availableTags,
                onTagAdded = { tag ->
                    appState.updateDefaultContextTags(appState.defaultContextTags + tag)
                },
                onTagRemoved = { tag ->
                    appState.updateDefaultContextTags(appState.defaultContextTags - tag)
                }
            )
        }

        // Time targets card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "Time Targets",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Daily target
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Daily target",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textOnCardPrimary,
                    modifier = Modifier.weight(1f)
                )
                var dailyText by remember(appState.dailyTargetHours) {
                    mutableStateOf(appState.dailyTargetHours.toString())
                }
                OutlinedTextField(
                    value = dailyText,
                    onValueChange = { value ->
                        dailyText = value.filter { it.isDigit() }
                        dailyText.toIntOrNull()?.let { appState.updateDailyTargetHours(it) }
                    },
                    modifier = Modifier.width(72.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderOnCard,
                        cursorColor = colors.accent
                    )
                )
                Text(
                    text = "hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textOnCardSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekly target
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Weekly target",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textOnCardPrimary,
                    modifier = Modifier.weight(1f)
                )
                var weeklyText by remember(appState.weeklyTargetHours) {
                    mutableStateOf(appState.weeklyTargetHours.toString())
                }
                OutlinedTextField(
                    value = weeklyText,
                    onValueChange = { value ->
                        weeklyText = value.filter { it.isDigit() }
                        weeklyText.toIntOrNull()?.let { appState.updateWeeklyTargetHours(it) }
                    },
                    modifier = Modifier.width(72.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderOnCard,
                        cursorColor = colors.accent
                    )
                )
                Text(
                    text = "hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textOnCardSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overtime tracking toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Overtime tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textOnCardPrimary
                    )
                    Text(
                        text = "Track hours worked beyond the daily target as overtime balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textOnCardSecondary
                    )
                }
                Switch(
                    checked = appState.overtimeEnabled,
                    onCheckedChange = { appState.updateOvertimeEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.cardSurface,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textOnCardTertiary,
                        uncheckedTrackColor = colors.borderOnCard
                    )
                )
            }

            if (appState.overtimeEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                // Start date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Start date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textOnCardPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    var startDateText by remember(appState.overtimeStartDate) {
                        mutableStateOf(appState.overtimeStartDate.toString())
                    }
                    var startDateError by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = startDateText,
                        onValueChange = { value ->
                            startDateText = value
                            val parsed = runCatching { LocalDate.parse(value) }
                            if (parsed.isSuccess) {
                                appState.updateOvertimeStartDate(parsed.getOrThrow())
                                startDateError = false
                            } else {
                                startDateError = true
                            }
                        },
                        modifier = Modifier.width(140.dp),
                        singleLine = true,
                        isError = startDateError,
                        placeholder = {
                            Text(
                                text = "YYYY-MM-DD",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textOnCardTertiary
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderOnCard,
                            errorBorderColor = colors.destructive,
                            cursorColor = colors.accent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Workday chips
                Text(
                    text = "Workdays",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textOnCardPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    DayOfWeek.entries.forEachIndexed { index, day ->
                        val isSelected = day in appState.workdays
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent else colors.borderOnCard.copy(alpha = 0.3f))
                                .clickable {
                                    val updated = if (isSelected) {
                                        appState.workdays - day
                                    } else {
                                        appState.workdays + day
                                    }
                                    appState.updateWorkdays(updated)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = dayLabels[index],
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else colors.textOnCardPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Excluded Days section
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Excluded Days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textOnCardPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Vacation, holidays, and other non-work days excluded from overtime calculation",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textOnCardSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Existing excluded ranges
                appState.excludedDateRanges.forEach { range ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        val sourceIcon = if (range.source == ExclusionSource.ABSENCE_IO) {
                            Icons.Outlined.Cloud
                        } else {
                            Icons.Outlined.Edit
                        }
                        Icon(
                            imageVector = sourceIcon,
                            contentDescription = range.source.name,
                            tint = colors.textOnCardTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val dateText = if (range.start == range.end) {
                            range.start.toString()
                        } else {
                            "${range.start} - ${range.end}"
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textOnCardPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (range.label.isNotBlank()) {
                            Text(
                                text = range.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textOnCardSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                appState.removeExcludedDateRange(range)
                                overtimeViewModel.refresh()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Remove",
                                tint = colors.destructive,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Add new excluded range
                Spacer(modifier = Modifier.height(8.dp))
                var newStartText by remember { mutableStateOf("") }
                var newEndText by remember { mutableStateOf("") }
                var newLabel by remember { mutableStateOf("") }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newStartText,
                        onValueChange = { newStartText = it },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        placeholder = {
                            Text("Start", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardTertiary)
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.textOnCardPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderOnCard,
                            cursorColor = colors.accent
                        )
                    )
                    OutlinedTextField(
                        value = newEndText,
                        onValueChange = { newEndText = it },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        placeholder = {
                            Text("End", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardTertiary)
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.textOnCardPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderOnCard,
                            cursorColor = colors.accent
                        )
                    )
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text("Label", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardTertiary)
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.textOnCardPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderOnCard,
                            cursorColor = colors.accent
                        )
                    )
                    OutlinedButton(
                        onClick = {
                            val start = runCatching { LocalDate.parse(newStartText) }.getOrNull()
                            val end = runCatching { LocalDate.parse(newEndText.ifBlank { newStartText }) }.getOrNull()
                            if (start != null && end != null) {
                                appState.addExcludedDateRange(
                                    ExcludedDateRange(start = start, end = end, label = newLabel)
                                )
                                overtimeViewModel.refresh()
                                newStartText = ""
                                newEndText = ""
                                newLabel = ""
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                    ) {
                        Text("Add", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // absence.io Integration card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "absence.io Integration",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-import absences",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textOnCardPrimary
                    )
                    Text(
                        text = "Sync approved absences from absence.io as excluded days",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textOnCardSecondary
                    )
                }
                Switch(
                    checked = appState.absenceIoEnabled,
                    onCheckedChange = { appState.updateAbsenceIoEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.cardSurface,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textOnCardTertiary,
                        uncheckedTrackColor = colors.borderOnCard
                    )
                )
            }

            if (appState.absenceIoEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                var keyIdText by remember(appState.absenceIoKeyId) {
                    mutableStateOf(appState.absenceIoKeyId)
                }
                OutlinedTextField(
                    value = keyIdText,
                    onValueChange = {
                        keyIdText = it
                        appState.updateAbsenceIoKeyId(it)
                    },
                    label = { Text("API Key ID", color = colors.textOnCardSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderOnCard,
                        cursorColor = colors.accent
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                var keySecretText by remember(appState.absenceIoKeySecret) {
                    mutableStateOf(appState.absenceIoKeySecret)
                }
                OutlinedTextField(
                    value = keySecretText,
                    onValueChange = {
                        keySecretText = it
                        appState.updateAbsenceIoKeySecret(it)
                    },
                    label = { Text("API Key Secret", color = colors.textOnCardSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderOnCard,
                        cursorColor = colors.accent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                var testResult by remember { mutableStateOf<String?>(null) }

                // Last sync + sync button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (appState.absenceIoLastSync.isNotBlank()) {
                            Text(
                                text = "Last sync: ${appState.absenceIoLastSync}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textOnCardSecondary
                            )
                        }
                        overtimeViewModel.syncError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.destructive
                            )
                        }
                        testResult?.let { result ->
                            val resultColor = if (result == "Connected") colors.success else colors.destructive
                            Text(
                                text = "Test: $result",
                                style = MaterialTheme.typography.bodySmall,
                                color = resultColor
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            testResult = null
                            overtimeViewModel.testAbsenceConnection { result ->
                                testResult = result.fold(
                                    onSuccess = { "Connected" },
                                    onFailure = { it.message ?: "Failed" }
                                )
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                    ) {
                        Text("Test", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { overtimeViewModel.syncAbsences() },
                        enabled = !overtimeViewModel.isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.bgPrimary
                        )
                    ) {
                        if (overtimeViewModel.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.bgPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Sync Now", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // PDF Reports card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "PDF Reports",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Directory where generated PDF reports will be stored",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnCardSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            var pdfDirText by remember(appState.pdfReportDir) {
                mutableStateOf(appState.pdfReportDir)
            }

            OutlinedTextField(
                value = pdfDirText,
                onValueChange = {
                    pdfDirText = it
                    appState.updatePdfReportDir(it)
                },
                label = { Text("Directory path", color = colors.textOnCardSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textOnCardPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderOnCard,
                    cursorColor = colors.accent
                )
            )
        }

        // AI Integration card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "AI Integration",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Connect to the AI backend for the Plan Todo feature",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnCardSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            var apiUrlText by remember(appState.apiBaseUrl) {
                mutableStateOf(appState.apiBaseUrl)
            }
            OutlinedTextField(
                value = apiUrlText,
                onValueChange = {
                    apiUrlText = it
                    appState.updateApiBaseUrl(it)
                },
                label = { Text("API URL", color = colors.textOnCardSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderOnCard,
                    cursorColor = colors.accent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            var apiTokenText by remember(appState.apiToken) {
                mutableStateOf(appState.apiToken)
            }
            OutlinedTextField(
                value = apiTokenText,
                onValueChange = {
                    apiTokenText = it
                    appState.updateApiToken(it)
                },
                label = { Text("API Token", color = colors.textOnCardSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderOnCard,
                    cursorColor = colors.accent
                )
            )
        }
    }
}
