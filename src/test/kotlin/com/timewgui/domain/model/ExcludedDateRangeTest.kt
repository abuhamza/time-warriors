package com.timewgui.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ExcludedDateRangeTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `serialize and deserialize MANUAL range`() {
        val range = ExcludedDateRange(
            start = LocalDate(2026, 3, 1),
            end = LocalDate(2026, 3, 5),
            label = "Vacation",
            source = ExclusionSource.MANUAL
        )
        val serialized = json.encodeToString(ExcludedDateRange.serializer(), range)
        val deserialized = json.decodeFromString<ExcludedDateRange>(serialized)
        assertEquals(range, deserialized)
    }

    @Test
    fun `serialize and deserialize ABSENCE_IO range`() {
        val range = ExcludedDateRange(
            start = LocalDate(2026, 6, 10),
            end = LocalDate(2026, 6, 20),
            label = "Summer holiday",
            source = ExclusionSource.ABSENCE_IO
        )
        val serialized = json.encodeToString(ExcludedDateRange.serializer(), range)
        val deserialized = json.decodeFromString<ExcludedDateRange>(serialized)
        assertEquals(range, deserialized)
        assertEquals(ExclusionSource.ABSENCE_IO, deserialized.source)
    }

    @Test
    fun `default source is MANUAL`() {
        val range = ExcludedDateRange(
            start = LocalDate(2026, 1, 1),
            end = LocalDate(2026, 1, 2)
        )
        assertEquals(ExclusionSource.MANUAL, range.source)
        assertEquals("", range.label)
    }
}
