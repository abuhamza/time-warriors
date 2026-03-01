package com.timewgui.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Duration

data class DailyOvertimeEntry(
    val date: LocalDate,
    val worked: Duration,
    val target: Duration,
    val isWorkday: Boolean,
    val overtime: Duration,
    val deficit: Duration,
    val isExcluded: Boolean = false,
)
