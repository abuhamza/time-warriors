package com.timewgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.timewgui.domain.system.LaunchAtLogin
import com.timewgui.ui.components.TagSelector
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.viewmodel.AppState
import com.timewgui.viewmodel.TagViewModel

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

@Composable
fun SettingsScreen(
    appState: AppState,
    tagViewModel: TagViewModel,
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

        // Daily target card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "Daily target",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "8h (placeholder)",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnCardSecondary
            )
        }

        // Weekly target card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "Weekly target",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "40h (placeholder)",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnCardSecondary
            )
        }

        // Working hours card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Text(
                text = "Working hours",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textOnCardPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnCardSecondary
            )
        }
    }
}
