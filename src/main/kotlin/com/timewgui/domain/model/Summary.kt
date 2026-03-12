package com.timewgui.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SummaryRequest(
    val period: String,
    val intervals: List<SummaryInterval>,
    val tasks: List<SummaryTask>,
    val totalDurationMinutes: Long,
    val dateRange: DateRange
)

@Serializable
data class SummaryTask(
    val title: String,
    val tag: String,
    val contextTags: List<String>,
    val status: String
)

@Serializable
data class SummaryInterval(
    val start: String,
    val end: String?,
    val tags: List<String>,
    val durationMinutes: Long
)

@Serializable
data class DateRange(
    val start: String,
    val end: String
)

@Serializable
data class SummaryResponse(
    val summary: String
)
