package com.timewgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.timewgui.domain.model.Interval
import com.timewgui.ui.components.DayTimeline
import com.timewgui.ui.components.IntervalDetailPanel
import com.timewgui.ui.components.IntervalList
import com.timewgui.ui.components.WeekTimeline
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.viewmodel.TimelineViewModel
import com.timewgui.viewmodel.ViewMode
import com.timewgui.viewmodel.TagViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun TimelineScreen(
    timelineViewModel: TimelineViewModel,
    tagViewModel: TagViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    var selectedInterval by remember { mutableStateOf<Interval?>(null) }

    LaunchedEffect(Unit) {
        timelineViewModel.switchViewMode(ViewMode.DAY)
        timelineViewModel.jumpToToday()
    }

    val (weekStart, _) = remember(timelineViewModel.selectedDate) {
        val daysSinceMonday = timelineViewModel.selectedDate.dayOfWeek.ordinal
        val start = timelineViewModel.selectedDate.minus(DatePeriod(days = daysSinceMonday))
        start to start.plus(DatePeriod(days = 7))
    }

    val intervalsByDay = remember(timelineViewModel.intervals, weekStart) {
        (0..6).map { i ->
            val day = weekStart.plus(DatePeriod(days = i))
            day to timelineViewModel.getIntervalsForDate(day)
        }.toMap()
    }

    val dailyTotals = remember(timelineViewModel.intervals, weekStart) {
        (0..6).map { i ->
            val day = weekStart.plus(DatePeriod(days = i))
            day to timelineViewModel.getDailyTotal(day)
        }.toMap()
    }

    Row(modifier = modifier.fillMaxSize().background(colors.bgPrimary)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(TimewDimensions.sectionGap),
            verticalArrangement = Arrangement.spacedBy(TimewDimensions.sectionGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { timelineViewModel.navigatePrevious() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bgTertiary),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text("◀", color = colors.textPrimary)
                    }
                    Text(
                        text = timelineViewModel.selectedDate.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Button(
                        onClick = { timelineViewModel.navigateNext() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bgTertiary),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text("▶", color = colors.textPrimary)
                    }
                    Button(
                        onClick = { timelineViewModel.jumpToToday() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text("Today", color = Color.White)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { timelineViewModel.switchViewMode(ViewMode.DAY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timelineViewModel.viewMode == ViewMode.DAY) colors.accent else colors.bgTertiary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text(
                            "Day",
                            color = if (timelineViewModel.viewMode == ViewMode.DAY) Color.White else colors.textPrimary
                        )
                    }
                    Button(
                        onClick = { timelineViewModel.switchViewMode(ViewMode.WEEK) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timelineViewModel.viewMode == ViewMode.WEEK) colors.accent else colors.bgTertiary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text(
                            "Week",
                            color = if (timelineViewModel.viewMode == ViewMode.WEEK) Color.White else colors.textPrimary
                        )
                    }
                }
            }

            if (timelineViewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else {
                when (timelineViewModel.viewMode) {
                    ViewMode.DAY -> {
                        val tagColorsMap = remember(timelineViewModel.intervals, tagViewModel.tagColors) {
                            timelineViewModel.intervals
                                .flatMap { it.tags }
                                .distinct()
                                .associateWith { tagViewModel.getColorForTag(it) }
                        }
                        DayTimeline(
                            date = timelineViewModel.selectedDate,
                            intervals = timelineViewModel.intervals,
                            tagColors = tagColorsMap,
                            onIntervalSelected = { selectedInterval = it },
                            onGapClicked = { _, _ -> }
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                                .background(colors.cardSurface)
                        ) {
                            IntervalList(
                                intervals = timelineViewModel.getIntervalsForDate(timelineViewModel.selectedDate),
                                getTagColor = tagViewModel::getColorForTag,
                                onIntervalClicked = { selectedInterval = it }
                            )
                        }
                    }
                    ViewMode.WEEK -> {
                        val weekTagColors = remember(intervalsByDay.values.flatten(), tagViewModel.tagColors) {
                            intervalsByDay.values
                                .flatMap { it.flatMap { i -> i.tags } }
                                .distinct()
                                .associateWith { tagViewModel.getColorForTag(it) }
                        }
                        WeekTimeline(
                            startDate = weekStart,
                            intervalsByDay = intervalsByDay,
                            tagColors = weekTagColors,
                            dailyTotals = dailyTotals,
                            onDayClicked = { timelineViewModel.jumpToDate(it) },
                            onIntervalSelected = { selectedInterval = it }
                        )
                    }
                }
            }
        }

        IntervalDetailPanel(
            interval = selectedInterval,
            visible = selectedInterval != null,
            availableTags = tagViewModel.availableTags,
            onSave = { id, startTime, endTime, tags, annotation ->
                selectedInterval?.let { interval ->
                    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
                    val localDate = interval.start.toLocalDateTime(tz).date
                    val dateStr = localDate.toString().replace("-", "")
                    val startStr = "${dateStr}T${startTime.replace(":", "")}00"
                    timelineViewModel.modifyStart(id, startStr)
                    endTime?.let { e ->
                        val endStr = "${dateStr}T${e.replace(":", "")}00"
                        timelineViewModel.modifyEnd(id, endStr)
                    }
                    timelineViewModel.replaceTags(id, tags)
                    annotation?.let { timelineViewModel.annotate(id, it) }
                }
                selectedInterval = null
            },
            onDelete = { id ->
                timelineViewModel.deleteInterval(id)
                selectedInterval = null
            },
            onSplit = { id ->
                timelineViewModel.splitInterval(id)
                selectedInterval = null
            },
            onDismiss = { selectedInterval = null }
        )
    }
}
