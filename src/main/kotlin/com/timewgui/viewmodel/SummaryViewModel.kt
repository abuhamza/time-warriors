package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timewgui.domain.api.AiToolsClient
import com.timewgui.domain.model.DateRange
import com.timewgui.domain.model.SummaryInterval
import com.timewgui.domain.model.SummaryRequest
import com.timewgui.domain.model.SummaryTask
import com.timewgui.domain.model.Interval
import com.timewgui.domain.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class SummaryPeriod(val label: String, val apiValue: String) {
    YESTERDAY("Yesterday", "yesterday"),
    THIS_WEEK("This Week", "this_week"),
    THIS_MONTH("This Month", "this_month")
}

class SummaryViewModel(
    private val appState: AppState,
    private val onError: (String) -> Unit = {}
) {
    var summary: String by mutableStateOf("")
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var selectedPeriod: SummaryPeriod? by mutableStateOf(null)
        private set

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private var currentJob: Job? = null

    fun generateSummary(period: SummaryPeriod, intervals: List<Interval>, tasks: List<Task>) {
        if (appState.apiToken.isBlank()) {
            onError("API token not configured. Set it in Settings → AI Integration.")
            return
        }

        currentJob?.cancel()
        selectedPeriod = period
        summary = ""

        val (startDate, endDate) = getDateRange(period)
        val tz = TimeZone.currentSystemDefault()
        val rangeStart = startDate.atStartOfDayIn(tz)
        val rangeEnd = endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)

        val filteredIntervals = intervals.filter { interval ->
            val intervalEnd = interval.end ?: Clock.System.now()
            interval.start < rangeEnd && intervalEnd > rangeStart
        }

        if (filteredIntervals.isEmpty()) {
            summary = "No tracked time for ${period.label.lowercase()}."
            return
        }

        val totalMinutes = filteredIntervals.fold(0L) { acc, i -> acc + i.duration.inWholeMinutes }

        // Find tasks whose tags appear in the filtered intervals
        val intervalTags = filteredIntervals.flatMap { it.tags }.toSet()
        val relevantTasks = tasks.filter { task ->
            task.tag in intervalTags || task.contextTags.any { it in intervalTags }
        }

        val request = SummaryRequest(
            period = period.apiValue,
            intervals = filteredIntervals.map { interval ->
                SummaryInterval(
                    start = interval.start.toString(),
                    end = interval.end?.toString(),
                    tags = interval.tags,
                    durationMinutes = interval.duration.inWholeMinutes
                )
            },
            tasks = relevantTasks.map { task ->
                SummaryTask(
                    title = task.title,
                    tag = task.tag,
                    contextTags = task.contextTags,
                    status = task.status.name
                )
            },
            totalDurationMinutes = totalMinutes,
            dateRange = DateRange(
                start = startDate.toString(),
                end = endDate.toString()
            )
        )

        currentJob = scope.launch {
            isLoading = true
            val client = AiToolsClient(appState.apiBaseUrl, appState.apiToken)
            client.generateSummary(request)
                .onSuccess { summary = it }
                .onFailure { e ->
                    summary = ""
                    onError(e.message ?: "Failed to generate summary")
                }
            isLoading = false
        }
    }

    fun clear() {
        currentJob?.cancel()
        summary = ""
        selectedPeriod = null
        isLoading = false
    }

    fun cancel() {
        currentJob?.cancel()
        job.cancel()
    }

    private fun getDateRange(period: SummaryPeriod): Pair<LocalDate, LocalDate> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return when (period) {
            SummaryPeriod.YESTERDAY -> {
                val yesterday = today.minus(DatePeriod(days = 1))
                yesterday to yesterday
            }
            SummaryPeriod.THIS_WEEK -> {
                val daysSinceMonday = today.dayOfWeek.ordinal
                val weekStart = today.minus(DatePeriod(days = daysSinceMonday))
                weekStart to today
            }
            SummaryPeriod.THIS_MONTH -> {
                val monthStart = LocalDate(today.year, today.month, 1)
                monthStart to today
            }
        }
    }
}
