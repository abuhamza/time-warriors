package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timewgui.domain.cli.TimewCli
import com.timewgui.domain.model.Interval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class ViewMode { DAY, WEEK }

class TimelineViewModel(
    private val timewCli: TimewCli,
    private val onError: (String) -> Unit = {}
) {
    var selectedDate: LocalDate by mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()))
        private set

    var viewMode: ViewMode by mutableStateOf(ViewMode.DAY)
        private set

    var intervals: List<Interval> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch { refreshIntervals() }
    }

    fun navigateNext() {
        selectedDate = when (viewMode) {
            ViewMode.DAY -> selectedDate.plus(DatePeriod(days = 1))
            ViewMode.WEEK -> selectedDate.plus(DatePeriod(days = 7))
        }
        scope.launch { refreshIntervals() }
    }

    fun navigatePrevious() {
        selectedDate = when (viewMode) {
            ViewMode.DAY -> selectedDate.minus(DatePeriod(days = 1))
            ViewMode.WEEK -> selectedDate.minus(DatePeriod(days = 7))
        }
        scope.launch { refreshIntervals() }
    }

    fun jumpToDate(date: LocalDate) {
        selectedDate = date
        scope.launch { refreshIntervals() }
    }

    fun jumpToToday() {
        selectedDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        scope.launch { refreshIntervals() }
    }

    fun switchViewMode(mode: ViewMode) {
        viewMode = mode
        scope.launch { refreshIntervals() }
    }

    fun fetchForRange(start: LocalDate, end: LocalDate) {
        scope.launch {
            isLoading = true
            val rangeEnd = end.plus(DatePeriod(days = 1))
            timewCli.exportIntervals(range = "$start - $rangeEnd", tags = emptyList())
                .onSuccess { intervals = it }
                .onFailure { e -> onError(e.message ?: "Failed to load intervals") }
            isLoading = false
        }
    }

    fun refreshIntervals() {
        scope.launch {
            isLoading = true
            timewCli.exportIntervals(range = computeExportRange(), tags = emptyList())
                .onSuccess { intervalList ->
                    intervals = intervalList
                }
                .onFailure { e -> onError(e.message ?: "Failed to load intervals") }
            isLoading = false
        }
    }

    fun deleteInterval(id: Int) {
        scope.launch {
            timewCli.deleteInterval(id)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to delete interval") }
        }
    }

    fun modifyStart(id: Int, newTime: String) {
        scope.launch {
            timewCli.modifyStart(id, newTime)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to modify start") }
        }
    }

    fun modifyEnd(id: Int, newTime: String) {
        scope.launch {
            timewCli.modifyEnd(id, newTime)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to modify end") }
        }
    }

    fun moveInterval(id: Int, newStart: String) {
        scope.launch {
            timewCli.moveInterval(id, newStart)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to move interval") }
        }
    }

    fun lengthen(id: Int, duration: String) {
        scope.launch {
            timewCli.lengthen(id, duration)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to lengthen interval") }
        }
    }

    fun shorten(id: Int, duration: String) {
        scope.launch {
            timewCli.shorten(id, duration)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to shorten interval") }
        }
    }

    fun splitInterval(id: Int) {
        scope.launch {
            timewCli.splitInterval(id)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to split interval") }
        }
    }

    fun joinIntervals(id1: Int, id2: Int) {
        scope.launch {
            timewCli.joinIntervals(id1, id2)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to join intervals") }
        }
    }

    fun replaceTags(id: Int, tags: List<String>) {
        scope.launch {
            timewCli.replaceTags(id, tags)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to update tags") }
        }
    }

    fun annotate(id: Int, annotation: String) {
        scope.launch {
            timewCli.annotate(id, annotation)
                .onSuccess { refreshIntervals() }
                .onFailure { e -> onError(e.message ?: "Failed to annotate") }
        }
    }

    fun getIntervalsForDate(date: LocalDate): List<Interval> {
        val tz = TimeZone.currentSystemDefault()
        val dayStart = date.atStartOfDayIn(tz)
        val dayEnd = date.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)

        return intervals.filter { interval ->
            val intervalEnd = interval.end ?: Clock.System.now()
            interval.start < dayEnd && intervalEnd > dayStart
        }
    }

    fun getDailyTotal(date: LocalDate): Duration {
        val tz = TimeZone.currentSystemDefault()
        val dayStart = date.atStartOfDayIn(tz)
        val dayEnd = date.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)
        val now = Clock.System.now()

        return getIntervalsForDate(date).fold(Duration.ZERO) { acc, interval ->
            val effectiveStart = maxOf(interval.start, dayStart)
            val effectiveEnd = interval.end?.let { minOf(it, dayEnd) }
                ?: minOf(now, dayEnd)
            val overlapMs = (effectiveEnd.toEpochMilliseconds() - effectiveStart.toEpochMilliseconds())
                .coerceAtLeast(0)
            acc + overlapMs.milliseconds
        }
    }

    fun getWeeklyTotal(): Duration {
        val (weekStart, weekEnd) = getWeekRange(selectedDate)
        val tz = TimeZone.currentSystemDefault()
        var total = 0L
        var d = weekStart
        while (d < weekEnd) {
            total += getDailyTotal(d).inWholeMilliseconds
            d = d.plus(DatePeriod(days = 1))
        }
        return total.milliseconds
    }

    private fun computeExportRange(): String {
        return when (viewMode) {
            ViewMode.DAY -> selectedDate.toString()
            ViewMode.WEEK -> {
                val (weekStart, weekEnd) = getWeekRange(selectedDate)
                "$weekStart - $weekEnd"
            }
        }
    }

    private fun getWeekRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val daysSinceMonday = date.dayOfWeek.ordinal
        val weekStart = date.minus(DatePeriod(days = daysSinceMonday))
        val weekEnd = weekStart.plus(DatePeriod(days = 7))
        return weekStart to weekEnd
    }
}

