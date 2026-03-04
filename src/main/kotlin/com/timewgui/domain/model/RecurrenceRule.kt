package com.timewgui.domain.model

import kotlinx.serialization.Serializable

enum class RecurrenceFrequency { DAILY, WEEKDAYS, WEEKLY }
enum class RecurrenceEndType { FOREVER, UNTIL_DATE, AFTER_OCCURRENCES }

@Serializable
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val daysOfWeek: Set<Int> = emptySet(), // 1=Mon..7=Sun, for WEEKLY custom
    val endType: RecurrenceEndType = RecurrenceEndType.FOREVER,
    val endDate: Long? = null,             // epoch millis, for UNTIL_DATE
    val maxOccurrences: Int? = null,       // for AFTER_OCCURRENCES
)
