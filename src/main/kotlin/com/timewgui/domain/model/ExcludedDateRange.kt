package com.timewgui.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ExcludedDateRange(
    val start: LocalDate,
    val end: LocalDate,
    val label: String = "",
    val source: ExclusionSource = ExclusionSource.MANUAL
)

@Serializable
enum class ExclusionSource { MANUAL, ABSENCE_IO }
