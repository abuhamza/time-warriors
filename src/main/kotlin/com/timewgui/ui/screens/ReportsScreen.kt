package com.timewgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.ui.theme.TimewTypography
import com.timewgui.viewmodel.TimelineViewModel
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration

sealed class ReportRange {
    data object Today : ReportRange()
    data object Week : ReportRange()
    data object Month : ReportRange()
    data object Year : ReportRange()
}

@Composable
fun ReportsScreen(
    timelineViewModel: TimelineViewModel,
    timerViewModel: com.timewgui.viewmodel.TimerViewModel,
    tagViewModel: com.timewgui.viewmodel.TagViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    var range by remember { mutableStateOf<ReportRange>(ReportRange.Week) }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val tz = TimeZone.currentSystemDefault()

    val (startDate, endDate) = remember(range, today) {
        when (range) {
            is ReportRange.Today -> today to today
            is ReportRange.Week -> {
                val daysSinceMonday = today.dayOfWeek.ordinal
                val start = today.minus(DatePeriod(days = daysSinceMonday))
                start to start.plus(DatePeriod(days = 6))
            }
            is ReportRange.Month -> {
                val start = LocalDate(today.year, today.month, 1)
                val end = start.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
                start to end
            }
            is ReportRange.Year -> {
                val start = LocalDate(today.year, 1, 1)
                val end = start.plus(DatePeriod(years = 1)).minus(DatePeriod(days = 1))
                start to end
            }
        }
    }

    val reportIntervals = remember(timelineViewModel.intervals, startDate, endDate) {
        timelineViewModel.intervals.filter { interval ->
            val intervalStart = interval.start.toLocalDateTime(tz).date
            intervalStart >= startDate && intervalStart <= endDate
        }
    }

    val totalDuration = remember(reportIntervals, timerViewModel.elapsedTime) {
        reportIntervals.fold(Duration.ZERO) { acc, i -> acc + i.duration }
    }

    LaunchedEffect(range) {
        timelineViewModel.fetchForRange(startDate, endDate)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgPrimary)
            .padding(TimewDimensions.sectionGap),
        verticalArrangement = Arrangement.spacedBy(TimewDimensions.sectionGap)
    ) {
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ":today" to ReportRange.Today,
                ":week" to ReportRange.Week,
                ":month" to ReportRange.Month,
                ":year" to ReportRange.Year,
            ).forEach { (label, r) ->
                Button(
                    onClick = { range = r },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (range == r) colors.accent else colors.bgTertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null
                ) {
                    Text(
                        label,
                        color = if (range == r) Color.White else colors.textPrimary
                    )
                }
            }
        }

        Text(
            text = "$startDate — $endDate",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textOnCardPrimary
                )
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textOnCardPrimary
                )
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textOnCardPrimary
                )
            }
            reportIntervals.forEach { interval ->
                val dateStr = interval.start.toLocalDateTime(tz).date.toString()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            drawLine(
                                color = colors.borderOnCard.copy(alpha = 0.3f),
                                start = Offset(0f, size.height - strokeWidth / 2),
                                end = Offset(size.width, size.height - strokeWidth / 2),
                                strokeWidth = strokeWidth
                            )
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateStr,
                        style = TimewTypography.monospace,
                        color = colors.textOnCardSecondary
                    )
                    Text(
                        text = interval.tags.joinToString(", ").ifEmpty { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textOnCardSecondary
                    )
                    Text(
                        text = interval.durationFormatted,
                        style = TimewTypography.monospace,
                        color = colors.textOnCardPrimary
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textOnCardPrimary
                )
                Text(
                    text = formatReportDuration(totalDuration),
                    style = TimewTypography.monospace,
                    color = colors.accent
                )
            }
        }

        val tagDurations = remember(reportIntervals) {
            val tagMap = mutableMapOf<String, Duration>()
            reportIntervals.forEach { interval ->
                interval.tags.forEach { tag ->
                    tagMap[tag] = (tagMap[tag] ?: Duration.ZERO) + interval.duration
                }
            }
            tagMap.entries.sortedByDescending { it.value }
        }

        if (tagDurations.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                    .background(colors.cardSurface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Time by Tag",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textOnCardPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val maxDuration = tagDurations.firstOrNull()?.value ?: Duration.ZERO
                tagDurations.forEach { (tag, duration) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textOnCardPrimary,
                            modifier = Modifier.width(120.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.borderOnCard.copy(alpha = 0.2f))
                        ) {
                            val fraction = if (maxDuration > Duration.ZERO) {
                                (duration.inWholeSeconds.toFloat() / maxDuration.inWholeSeconds.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tagViewModel.getColorForTag(tag))
                            )
                        }
                        Text(
                            text = formatReportDuration(duration),
                            style = TimewTypography.monospace,
                            color = colors.textOnCardSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun formatReportDuration(d: Duration): String {
    val totalMinutes = d.inWholeMinutes
    if (totalMinutes < 60) return "${totalMinutes}m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
}
