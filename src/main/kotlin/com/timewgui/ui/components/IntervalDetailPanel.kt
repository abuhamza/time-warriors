package com.timewgui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timewgui.domain.model.Interval
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.ui.theme.TimewTypography
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime

private val PanelWidth = 300.dp
private val AnimationDuration = 150
private val SectionLabelStyle = 11.sp

@OptIn(FormatStringsInDatetimeFormats::class)
private val timeFormat = LocalDateTime.Format { byUnicodePattern("HH:mm") }

@OptIn(FormatStringsInDatetimeFormats::class)
private val dateFormat = LocalDateTime.Format { byUnicodePattern("MM/dd/yyyy") }

fun formatInstantToTime(instant: Instant): String {
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return timeFormat.format(ldt)
}

fun formatInstantToDate(instant: Instant): String {
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return dateFormat.format(ldt)
}

@Composable
fun IntervalDetailPanel(
    interval: Interval?,
    visible: Boolean,
    availableTags: List<String>,
    onSave: (id: Int, startTime: String, endTime: String?, tags: List<String>, annotation: String?) -> Unit,
    onDelete: (id: Int) -> Unit,
    onSplit: (id: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current

    AnimatedVisibility(
        visible = visible && interval != null,
        enter = slideInHorizontally(
            animationSpec = tween(AnimationDuration, easing = EaseOut),
            initialOffsetX = { it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(AnimationDuration, easing = EaseOut),
            targetOffsetX = { it }
        ),
        modifier = modifier
    ) {
        interval?.let { iv ->
            var startTime by remember(iv) { mutableStateOf(formatInstantToTime(iv.start)) }
            var endTime by remember(iv) { mutableStateOf(iv.end?.let { formatInstantToTime(it) } ?: "") }
            var tags by remember(iv) { mutableStateOf(iv.tags.toList()) }
            var annotation by remember(iv) { mutableStateOf(iv.annotation ?: "") }

            Column(
                modifier = Modifier
                    .width(PanelWidth)
                    .fillMaxHeight()
                    .background(colors.bgPrimary)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = colors.border,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                    .padding(TimewDimensions.sectionGap)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(TimewDimensions.sectionGap)
            ) {
                Text(
                    text = "EDIT INTERVAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                SectionLabel("Start time")
                EditableTimeField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    placeholder = "HH:mm",
                    enabled = true,
                    colors = colors
                )

                SectionLabel("End time")
                EditableTimeField(
                    value = if (iv.isActive) "—" else endTime,
                    onValueChange = { endTime = it },
                    placeholder = "HH:mm",
                    enabled = !iv.isActive,
                    colors = colors
                )

                SectionLabel("Duration")
                Text(
                    text = iv.durationFormatted,
                    style = TimewTypography.monospace,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                SectionLabel("Date")
                Text(
                    text = formatInstantToDate(iv.start),
                    style = TimewTypography.monospace,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                SectionLabel("Tags")
                TagSelector(
                    selectedTags = tags,
                    availableTags = availableTags,
                    onTagAdded = { tags = tags + it },
                    onTagRemoved = { tags = tags - it }
                )

                SectionLabel("Annotation")
                BasicTextField(
                    value = annotation,
                    onValueChange = { annotation = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgTertiary, RoundedCornerShape(TimewDimensions.borderRadiusInput))
                        .padding(12.dp)
                        .height(80.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    decorationBox = { innerTextField ->
                        Column {
                            if (annotation.isEmpty()) {
                                Text(
                                    text = "Add annotation...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textTertiary
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(TimewDimensions.sectionGap))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onSplit(iv.id) }) {
                            Text(
                                text = "Split",
                                color = colors.textSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        DeleteButton(
                            onClick = { onDelete(iv.id) },
                            colors = colors
                        )
                    }
                    Button(
                        onClick = {
                            onSave(
                                iv.id,
                                startTime,
                                if (iv.isActive) null else endTime.takeIf { it.isNotBlank() },
                                tags,
                                annotation.ifBlank { null }
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontSize = SectionLabelStyle,
        color = LocalTimewColors.current.textSecondary,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun EditableTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    colors: com.timewgui.ui.theme.TimewColors
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (enabled) colors.bgTertiary else colors.bgPrimary,
                RoundedCornerShape(TimewDimensions.borderRadiusInput)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        textStyle = TimewTypography.monospace.copy(
            color = if (enabled) colors.textPrimary else colors.textTertiary
        ),
        cursorBrush = SolidColor(colors.accent),
        singleLine = true,
        decorationBox = { innerTextField ->
            if (value.isEmpty() && enabled) {
                Text(
                    text = placeholder,
                    style = TimewTypography.monospace,
                    color = colors.textTertiary
                )
            }
            innerTextField()
        }
    )
}

@Composable
private fun DeleteButton(
    onClick: () -> Unit,
    colors: com.timewgui.ui.theme.TimewColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value

    TextButton(
        onClick = onClick,
        modifier = Modifier.hoverable(interactionSource = interactionSource)
    ) {
        Text(
            text = "Delete",
            color = if (isHovered) colors.destructive else colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
