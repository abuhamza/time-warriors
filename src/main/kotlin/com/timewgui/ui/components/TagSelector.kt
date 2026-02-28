package com.timewgui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions

@Composable
fun TagSelector(
    selectedTags: List<String>,
    availableTags: List<String>,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val colors = LocalTimewColors.current
    val focusManager = LocalFocusManager.current

    val query = inputText.trim()
    val suggestions = remember(query, availableTags, selectedTags) {
        if (query.isEmpty()) {
            availableTags.filter { it !in selectedTags }
        } else {
            availableTags
                .filter { it !in selectedTags && it.contains(query, ignoreCase = true) }
                .take(10)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedTags.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(selectedTags) { tag ->
                    TagChip(
                        tag = tag,
                        onRemove = { onTagRemoved(tag) },
                        colors = colors
                    )
                }
            }
        }

        Box {
            BasicTextField(
                value = inputText,
                onValueChange = { newValue ->
                    inputText = newValue
                    isDropdownExpanded = newValue.isNotBlank()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            val text = inputText.trim()
                            if (text.isNotEmpty()) {
                                onTagAdded(text)
                                inputText = ""
                                isDropdownExpanded = false
                                focusManager.clearFocus()
                            }
                            true
                        } else {
                            false
                        }
                    }
                    .background(colors.bgTertiary, RoundedCornerShape(TimewDimensions.borderRadiusInput))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Add tag...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textTertiary
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (isDropdownExpanded && (suggestions.isNotEmpty() || query.isNotEmpty())) {
                Popup(
                    onDismissRequest = { isDropdownExpanded = false },
                    properties = PopupProperties(focusable = false)
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 200.dp, max = 280.dp)
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.bgSecondary)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        suggestions.forEach { tag ->
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onTagAdded(tag)
                                        inputText = ""
                                        isDropdownExpanded = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        if (query.isNotEmpty() && query !in availableTags && query !in selectedTags) {
                            Text(
                                text = "Add \"$query\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.accent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onTagAdded(query)
                                        inputText = ""
                                        isDropdownExpanded = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: String,
    onRemove: () -> Unit,
    colors: com.timewgui.ui.theme.TimewColors
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accent.copy(alpha = 0.15f))
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelMedium,
            color = colors.accent
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove tag",
                tint = colors.accent.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
