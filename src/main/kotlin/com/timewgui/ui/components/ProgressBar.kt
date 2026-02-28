package com.timewgui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewTypography
import kotlin.time.Duration

private val BarHeight = 6.dp
private val BarShape = RoundedCornerShape(3.dp)

/**
 * Formats a [Duration] to a compact string (e.g., "7h 16m", "45m").
 */
fun formatDurationCompact(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    if (totalMinutes < 60) return "${totalMinutes}m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
}

@Composable
fun ProgressIndicator(
    label: String,
    current: Duration,
    target: Duration,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value

    val progress = if (target.inWholeMilliseconds > 0) {
        (current.inWholeMilliseconds.toFloat() / target.inWholeMilliseconds).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = "${formatDurationCompact(current)} / ${formatDurationCompact(target)}",
                style = TimewTypography.monospace,
                color = colors.textPrimary,
                fontSize = 12.sp
            )
            if (isHovered && target.inWholeMilliseconds > 0) {
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = TimewTypography.monospace,
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .clip(BarShape)
                .background(colors.bgTertiary)
        ) {
            if (progress > 0f) {
                Spacer(
                    modifier = Modifier
                        .height(BarHeight)
                        .fillMaxWidth(progress)
                        .clip(BarShape)
                        .background(colors.accent)
                )
            }
        }
    }
}
