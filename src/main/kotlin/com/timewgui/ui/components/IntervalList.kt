package com.timewgui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timewgui.domain.model.Interval
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewTypography

private val RowPaddingH = 16.dp
private val RowPaddingV = 12.dp
private val ColorBarWidth = 24.dp
private val ColorBarHeight = 3.dp

@Composable
fun IntervalList(
    intervals: List<Interval>,
    tagColors: Map<String, Color>,
    onIntervalClicked: (Interval) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultTagColor = LocalTimewColors.current.textSecondary
    IntervalListInternal(
        intervals = intervals,
        getTagColor = { tag -> tagColors[tag] ?: defaultTagColor },
        onIntervalClicked = onIntervalClicked,
        modifier = modifier
    )
}

/** Overload for use with TagViewModel.getColorForTag. */
@Composable
fun IntervalList(
    intervals: List<Interval>,
    getTagColor: (String) -> Color,
    onIntervalClicked: (Interval) -> Unit,
    modifier: Modifier = Modifier
) {
    IntervalListInternal(
        intervals = intervals,
        getTagColor = getTagColor,
        onIntervalClicked = onIntervalClicked,
        modifier = modifier
    )
}

@Composable
private fun IntervalListInternal(
    intervals: List<Interval>,
    getTagColor: (String) -> Color,
    onIntervalClicked: (Interval) -> Unit,
    modifier: Modifier
) {
    val colors = LocalTimewColors.current

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(intervals, key = { it.id }) { interval ->
            IntervalRow(
                interval = interval,
                getTagColor = getTagColor,
                colors = colors,
                onClick = { onIntervalClicked(interval) }
            )
        }
    }
}

@Composable
private fun IntervalRow(
    interval: Interval,
    getTagColor: (String) -> Color,
    colors: com.timewgui.ui.theme.TimewColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value
    val primaryTag = interval.tags.firstOrNull() ?: ""
    val tagColor = if (primaryTag.isNotEmpty()) getTagColor(primaryTag) else colors.textTertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .hoverable(interactionSource = interactionSource)
            .then(
                if (isHovered) Modifier.background(colors.borderOnCard.copy(alpha = 0.15f))
                else Modifier
            )
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = colors.borderOnCard.copy(alpha = 0.3f),
                    start = Offset(RowPaddingH.toPx(), size.height - strokeWidth / 2),
                    end = Offset(size.width - RowPaddingH.toPx(), size.height - strokeWidth / 2),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = RowPaddingH, vertical = RowPaddingV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored bar indicator
        Box(
            modifier = Modifier
                .width(ColorBarWidth)
                .height(ColorBarHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(tagColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Tag name and annotation
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (interval.isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.success)
                )
            }
            Text(
                text = if (primaryTag.isNotEmpty()) primaryTag else formatIntervalTimeRange(interval),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textOnCardPrimary
            )
            if (interval.annotation != null) {
                Text(
                    text = interval.annotation,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textOnCardTertiary
                )
            }
        }

        // Duration
        Text(
            text = interval.durationFormatted,
            style = TimewTypography.monospace,
            color = colors.textOnCardSecondary
        )
    }
}

private fun formatIntervalTimeRange(interval: Interval): String {
    val startStr = formatInstantToTime(interval.start)
    val endStr = if (interval.end != null) {
        formatInstantToTime(interval.end)
    } else {
        "now"
    }
    return "$startStr — $endStr"
}
