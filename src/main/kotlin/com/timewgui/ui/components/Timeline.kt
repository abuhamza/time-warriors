package com.timewgui.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.timewgui.domain.model.Interval
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewTypography
import kotlin.time.Duration

private val LabelHeight = 24.dp
private val BlocksHeight = 80.dp
private val MinBlockWidthForTag = 48.dp
private val ActiveBorderWidthPx = 2f
private val CurrentTimeLineWidthPx = 1f
private val PulsingDotRadius = 4f

/**
 * Horizontal day timeline showing intervals from 00:00 to 24:00 (or configurable hours).
 * Uses Canvas for rendering. Click intervals to select, click gaps to fill.
 */
@Composable
fun DayTimeline(
    date: kotlinx.datetime.LocalDate,
    intervals: List<Interval>,
    tagColors: Map<String, Color>,
    onIntervalSelected: (Interval) -> Unit,
    onGapClicked: (gapStart: Instant, gapEnd: Instant) -> Unit,
    modifier: Modifier = Modifier,
    startHour: Int = 0,
    endHour: Int = 24
) {
    val tz = TimeZone.currentSystemDefault()
    val density = LocalDensity.current
    val colors = LocalTimewColors.current

    val dayStart = date.atStartOfDayIn(tz)
    val dayEnd = date.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)
    val axisStart = dayStart.plus(startHour * 3600 * 1000L, DateTimeUnit.MILLISECOND)
    val axisEnd = dayStart.plus(endHour * 3600 * 1000L, DateTimeUnit.MILLISECOND)

    val filteredIntervals = remember(intervals, date, tz) {
        intervals.filter { interval ->
            val end = interval.end ?: Clock.System.now()
            interval.start < dayEnd && end > dayStart
        }.sortedBy { it.start }
    }

    val (displayIntervals, gaps) = remember(filteredIntervals, dayStart, dayEnd, axisStart, axisEnd) {
        computeIntervalsAndGaps(filteredIntervals, dayStart, dayEnd, axisStart, axisEnd)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val totalHours = (endHour - startHour).coerceAtLeast(1)

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LabelHeight)
            ) {
                (startHour..endHour).forEach { hour ->
                    val fraction = (hour - startHour).toFloat() / totalHours
                    val xPx = fraction * totalWidthPx
                    Text(
                        text = "%02d:00".format(hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = with(density) { xPx.toDp() })
                    )
                }
            }

            DayTimelineContent(
                date = date,
                displayIntervals = displayIntervals,
                gaps = gaps,
                tagColors = tagColors,
                onIntervalSelected = onIntervalSelected,
                onGapClicked = onGapClicked,
                pulseAlpha = pulseAlpha,
                startHour = startHour,
                endHour = endHour,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BlocksHeight)
            )
        }
    }
}

private data class DisplayInterval(
    val interval: Interval,
    val startTime: Instant,
    val endTime: Instant,
    val clipStart: Instant,
    val clipEnd: Instant
)

private data class Gap(
    val start: Instant,
    val end: Instant
)

private fun computeIntervalsAndGaps(
    intervals: List<Interval>,
    dayStart: Instant,
    dayEnd: Instant,
    axisStart: Instant,
    axisEnd: Instant
): Pair<List<DisplayInterval>, List<Gap>> {
    val now = Clock.System.now()
    val displayIntervals = mutableListOf<DisplayInterval>()
    val gaps = mutableListOf<Gap>()

    var lastEnd = axisStart

    for (interval in intervals) {
        val effectiveStart = maxOf(interval.start, dayStart)
        val effectiveEnd = interval.end?.let { minOf(it, dayEnd) }
            ?: minOf(now, dayEnd)

        if (effectiveStart >= effectiveEnd) continue

        val clipStart = maxOf(effectiveStart, axisStart)
        val clipEnd = minOf(effectiveEnd, axisEnd)

        if (clipStart < clipEnd) {
            if (lastEnd < effectiveStart) {
                val gapStart = maxOf(lastEnd, axisStart)
                val gapEnd = minOf(effectiveStart, axisEnd)
                if (gapStart < gapEnd) {
                    gaps.add(Gap(gapStart, gapEnd))
                }
            }

            displayIntervals.add(
                DisplayInterval(
                    interval = interval,
                    startTime = effectiveStart,
                    endTime = effectiveEnd,
                    clipStart = clipStart,
                    clipEnd = clipEnd
                )
            )
            lastEnd = effectiveEnd
        }
    }

    if (lastEnd < axisEnd) {
        gaps.add(Gap(maxOf(lastEnd, axisStart), axisEnd))
    }

    return displayIntervals to gaps
}

@Composable
private fun DayTimelineContent(
    date: kotlinx.datetime.LocalDate,
    displayIntervals: List<DisplayInterval>,
    gaps: List<Gap>,
    tagColors: Map<String, Color>,
    onIntervalSelected: (Interval) -> Unit,
    onGapClicked: (gapStart: Instant, gapEnd: Instant) -> Unit,
    pulseAlpha: Float,
    startHour: Int,
    endHour: Int,
    modifier: Modifier
) {
    val tz = TimeZone.currentSystemDefault()
    val colors = LocalTimewColors.current
    val dayStart = date.atStartOfDayIn(tz)
    val axisStartInstant = dayStart.plus(startHour * 3600 * 1000L, DateTimeUnit.MILLISECOND)
    val axisEndInstant = dayStart.plus(endHour * 3600 * 1000L, DateTimeUnit.MILLISECOND)
    val totalMinutes = (endHour - startHour).coerceAtLeast(1) * 60.0

    Canvas(
        modifier = modifier
            .pointerInput(displayIntervals, gaps, onIntervalSelected, onGapClicked) {
                detectTapGestures { offset ->
                    val width = size.width
                    if (width <= 0f) return@detectTapGestures
                    val xFraction = (offset.x / width).coerceIn(0f, 1f)
                    val minutesFromStart = xFraction * totalMinutes.toFloat()
                    val clickedInstant = axisStartInstant.plus(
                        (minutesFromStart * 60 * 1000).toLong(),
                        DateTimeUnit.MILLISECOND
                    )

                    val hitInterval = displayIntervals.find { di ->
                        clickedInstant >= di.clipStart && clickedInstant <= di.clipEnd
                    }
                    val hitGap = gaps.find { gap ->
                        clickedInstant >= gap.start && clickedInstant <= gap.end
                    }
                    when {
                        hitInterval != null -> onIntervalSelected(hitInterval.interval)
                        hitGap != null -> onGapClicked(hitGap.start, hitGap.end)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val axisStartMs = axisStartInstant.toEpochMilliseconds()
        val axisEndMs = axisEndInstant.toEpochMilliseconds()
        fun Instant.toX(): Float {
            val ms = toEpochMilliseconds()
            return ((ms - axisStartMs).toFloat() / (axisEndMs - axisStartMs)) * w
        }

        // Hour grid lines
        for (hour in startHour..endHour) {
            val instant = axisStartInstant.plus((hour - startHour) * 3600 * 1000L, DateTimeUnit.MILLISECOND)
            val x = instant.toX()
            if (x in 0f..w) {
                drawLine(
                    color = colors.border,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }
        }

        // Gaps
        gaps.forEach { gap ->
            val left = gap.start.toX()
            val right = gap.end.toX()
            if (right > left) {
                drawRect(color = colors.bgTertiary, topLeft = Offset(left, 0f), size = Size(right - left, h))
                drawLine(
                    color = colors.border,
                    start = Offset(left, 0f),
                    end = Offset(right, 0f),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
                drawLine(
                    color = colors.border,
                    start = Offset(left, h),
                    end = Offset(right, h),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }
        }

        // Intervals
        displayIntervals.forEach { di ->
            val left = di.clipStart.toX()
            val right = di.clipEnd.toX()
            val blockWidth = right - left
            val tag = di.interval.tags.firstOrNull() ?: ""
            val color = tagColors[tag] ?: colors.accent.copy(alpha = 0.6f)

            drawRect(color = color, topLeft = Offset(left, 0f), size = Size(blockWidth, h))

            if (di.interval.isActive) {
                drawLine(
                    color = colors.success,
                    start = Offset(left, 0f),
                    end = Offset(left, h),
                    strokeWidth = ActiveBorderWidthPx
                )
                val dotY = h / 2f
                drawCircle(
                    color = colors.success.copy(alpha = pulseAlpha),
                    radius = PulsingDotRadius,
                    center = Offset(left + PulsingDotRadius + 2, dotY)
                )
            }
        }

        // Current time line
        val now = Clock.System.now()
        if (now >= axisStartInstant && now <= axisEndInstant) {
            val nowX = now.toX()
            drawLine(
                color = colors.destructive,
                start = Offset(nowX, 0f),
                end = Offset(nowX, h),
                strokeWidth = CurrentTimeLineWidthPx
            )
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val density = LocalDensity.current
        val axisStartMs = axisStartInstant.toEpochMilliseconds()
        val axisEndMs = axisEndInstant.toEpochMilliseconds()
        val axisRangeMs = (axisEndMs - axisStartMs).toFloat().coerceAtLeast(1f)

        displayIntervals.forEach { di ->
            val leftPx = ((di.clipStart.toEpochMilliseconds() - axisStartMs).toFloat() / axisRangeMs) * widthPx
            val blockWidthPx = ((di.clipEnd.toEpochMilliseconds() - di.clipStart.toEpochMilliseconds()).toFloat() / axisRangeMs) * widthPx
            val blockWidthDp = with(density) { blockWidthPx.toDp() }
            val tag = di.interval.tags.firstOrNull() ?: ""

            if (blockWidthDp >= MinBlockWidthForTag && tag.isNotEmpty()) {
                val leftDp = with(density) { leftPx.toDp() }
                @OptIn(ExperimentalFoundationApi::class)
                TooltipArea(
                    tooltip = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.bgSecondary.copy(alpha = 0.95f)
                        ) {
                            val startLdt = di.startTime.toLocalDateTime(tz)
                            val endLdt = di.clipEnd.toLocalDateTime(tz)
                            Text(
                                text = buildString {
                                    append(tag)
                                    if (di.interval.tags.size > 1) append(", ${di.interval.tags.drop(1).joinToString()}")
                                    append("\n")
                                    append("%02d:%02d - %02d:%02d".format(
                                        startLdt.hour, startLdt.minute,
                                        endLdt.hour, endLdt.minute
                                    ))
                                    append("\n${di.interval.durationFormatted}")
                                    di.interval.annotation?.let { append("\n$it") }
                                },
                                color = colors.textPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    },
                    tooltipPlacement = TooltipPlacement.CursorPoint(
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset(0.dp, 8.dp)
                    ),
                    delayMillis = 400
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = leftDp)
                            .height(BlocksHeight)
                            .widthIn(min = blockWidthDp)
                            .padding(horizontal = 4.dp)
                            .clickable { onIntervalSelected(di.interval) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Week view: 7 rows (Mon-Sun), each row is a mini day timeline.
 * Daily totals right-aligned in monospace. Click a day row to drill into Day view.
 */
@Composable
fun WeekTimeline(
    startDate: LocalDate,
    intervalsByDay: Map<LocalDate, List<Interval>>,
    tagColors: Map<String, Color>,
    dailyTotals: Map<LocalDate, Duration>,
    onDayClicked: (LocalDate) -> Unit,
    onIntervalSelected: (Interval) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val colors = LocalTimewColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        repeat(7) { i ->
            val day = startDate.plus(DatePeriod(days = i))
            val dayIntervals = intervalsByDay[day] ?: emptyList()
            val total = dailyTotals[day] ?: Duration.ZERO
            val totalStr = formatDurationForDisplay(total)
            val tz = TimeZone.currentSystemDefault()
            val (displayIntervals, gaps) = remember(dayIntervals, day) {
                val dayStart = day.atStartOfDayIn(tz)
                val dayEnd = day.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)
                val axisStart = dayStart.plus(0L, DateTimeUnit.MILLISECOND)
                val axisEnd = dayStart.plus(24 * 3600 * 1000L, DateTimeUnit.MILLISECOND)
                computeIntervalsAndGaps(
                    dayIntervals.sortedBy { it.start },
                    dayStart, dayEnd, axisStart, axisEnd
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BlocksHeight + LabelHeight + 8.dp)
                    .clickable { onDayClicked(day) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayLabels[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.widthIn(min = 32.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(BlocksHeight)
                ) {
                    DayTimelineContent(
                        date = day,
                        displayIntervals = displayIntervals,
                        gaps = gaps,
                        tagColors = tagColors,
                        onIntervalSelected = onIntervalSelected,
                        onGapClicked = { _, _ -> },
                        pulseAlpha = 1f,
                        startHour = 0,
                        endHour = 24,
                        modifier = Modifier.fillMaxWidth().height(BlocksHeight)
                    )
                }
                Text(
                    text = totalStr,
                    style = TimewTypography.monospace,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

private fun formatDurationForDisplay(d: Duration): String {
    val totalMinutes = d.inWholeMinutes
    if (totalMinutes < 60) return "${totalMinutes}m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
}
