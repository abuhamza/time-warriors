package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timewgui.domain.api.AbsenceIoClient
import com.timewgui.domain.cli.TimewCli
import com.timewgui.domain.model.DailyOvertimeEntry
import com.timewgui.domain.model.ExcludedDateRange
import com.timewgui.domain.model.ExclusionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class OvertimeViewModel(
    private val timewCli: TimewCli,
    private val appState: AppState,
    private val onError: (String) -> Unit = {}
) {
    var netBalance: Duration by mutableStateOf(Duration.ZERO)
        private set

    var totalOvertime: Duration by mutableStateOf(Duration.ZERO)
        private set

    var totalDeficit: Duration by mutableStateOf(Duration.ZERO)
        private set

    var todayBalance: Duration by mutableStateOf(Duration.ZERO)
        private set

    var entries: List<DailyOvertimeEntry> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var unreviewedDays: List<LocalDate> by mutableStateOf(emptyList())
        private set

    var isSyncing: Boolean by mutableStateOf(false)
        private set

    var syncError: String? by mutableStateOf(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun buildExcludedDatesSet(): Set<LocalDate> {
        val set = mutableSetOf<LocalDate>()
        for (range in appState.excludedDateRanges) {
            var d = range.start
            while (d <= range.end) {
                set.add(d)
                d = d.plus(DatePeriod(days = 1))
            }
        }
        return set
    }

    fun refresh() {
        if (!appState.overtimeEnabled) return
        scope.launch {
            isLoading = true
            val tz = TimeZone.currentSystemDefault()
            val today = Clock.System.todayIn(tz)
            val startDate = appState.overtimeStartDate
            val tomorrow = today.plus(DatePeriod(days = 1))
            val dailyTarget = appState.dailyTargetHours.hours
            val workdays = appState.workdays
            val excludedDates = buildExcludedDatesSet()

            timewCli.exportIntervals(range = "$startDate - $tomorrow")
                .onSuccess { intervals ->
                    val result = mutableListOf<DailyOvertimeEntry>()
                    val unreviewed = mutableListOf<LocalDate>()
                    var date = startDate
                    while (date <= today) {
                        val dayStart = date.atStartOfDayIn(tz)
                        val dayEnd = date.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)
                        val now = Clock.System.now()

                        val worked = intervals
                            .filter { interval ->
                                val intervalEnd = interval.end ?: now
                                interval.start < dayEnd && intervalEnd > dayStart
                            }
                            .fold(Duration.ZERO) { acc, interval ->
                                val effectiveStart = maxOf(interval.start, dayStart)
                                val effectiveEnd = interval.end?.let { minOf(it, dayEnd) }
                                    ?: minOf(now, dayEnd)
                                val ms = (effectiveEnd.toEpochMilliseconds() - effectiveStart.toEpochMilliseconds())
                                    .coerceAtLeast(0)
                                acc + ms.milliseconds
                            }

                        val isExcluded = date in excludedDates
                        val isWorkday = date.dayOfWeek in workdays && !isExcluded
                        val target = if (isWorkday) dailyTarget else Duration.ZERO
                        val overtime = if (worked > target) worked - target else Duration.ZERO
                        val deficit = if (isWorkday && target > worked) target - worked else Duration.ZERO

                        result.add(
                            DailyOvertimeEntry(
                                date = date,
                                worked = worked,
                                target = target,
                                isWorkday = isWorkday,
                                overtime = overtime,
                                deficit = deficit,
                                isExcluded = isExcluded,
                            )
                        )

                        if (date.dayOfWeek in appState.workdays && !isExcluded &&
                            worked == Duration.ZERO && date < today
                        ) {
                            unreviewed.add(date)
                        }

                        date = date.plus(DatePeriod(days = 1))
                    }

                    entries = result
                    unreviewedDays = unreviewed
                    totalOvertime = result.fold(Duration.ZERO) { acc, e -> acc + e.overtime }
                    totalDeficit = result.fold(Duration.ZERO) { acc, e -> acc + e.deficit }
                    netBalance = totalOvertime - totalDeficit
                    todayBalance = result.lastOrNull()?.let { it.overtime - it.deficit } ?: Duration.ZERO
                }
                .onFailure { e -> onError(e.message ?: "Failed to calculate overtime") }

            isLoading = false
        }
    }

    fun syncAbsences() {
        if (!appState.absenceIoEnabled) return
        if (appState.absenceIoKeyId.isBlank() || appState.absenceIoKeySecret.isBlank()) return
        scope.launch {
            isSyncing = true
            syncError = null
            try {
                val client = AbsenceIoClient(appState.absenceIoKeyId, appState.absenceIoKeySecret)
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                client.fetchAbsences(appState.overtimeStartDate, today)
                    .onSuccess { absences ->
                        val importedRanges = absences.mapNotNull { entry ->
                            val start = runCatching { LocalDate.parse(entry.start.take(10)) }.getOrNull()
                            val end = runCatching { LocalDate.parse(entry.end.take(10)) }.getOrNull()
                            if (start != null && end != null) {
                                ExcludedDateRange(
                                    start = start,
                                    end = end,
                                    label = entry.reason?.name ?: "Absence",
                                    source = ExclusionSource.ABSENCE_IO
                                )
                            } else null
                        }
                        val manualRanges = appState.excludedDateRanges.filter {
                            it.source == ExclusionSource.MANUAL
                        }
                        appState.updateExcludedDateRanges(manualRanges + importedRanges)
                        appState.updateAbsenceIoLastSync(
                            java.time.LocalDateTime.now().toString().take(16).replace('T', ' ')
                        )
                        refresh()
                    }
                    .onFailure { e ->
                        syncError = e.message ?: "Sync failed"
                        onError("absence.io sync failed: ${e.message}")
                    }
            } finally {
                isSyncing = false
            }
        }
    }

    fun testAbsenceConnection(onResult: (Result<Unit>) -> Unit) {
        if (appState.absenceIoKeyId.isBlank() || appState.absenceIoKeySecret.isBlank()) {
            onResult(Result.failure(Exception("API credentials are empty")))
            return
        }
        scope.launch {
            val client = AbsenceIoClient(appState.absenceIoKeyId, appState.absenceIoKeySecret)
            val result = client.testConnection()
            onResult(result)
        }
    }
}
