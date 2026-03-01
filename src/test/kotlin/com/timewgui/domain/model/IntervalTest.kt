package com.timewgui.domain.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Instant

class IntervalTest {

    private val json = Json { ignoreUnknownKeys = true }

    // --- TimewarriorDateFormat ---

    @Test
    fun `parse produces correct Instant`() {
        val instant = TimewarriorDateFormat.parse("20260227T074517Z")
        assertEquals(Instant.parse("2026-02-27T07:45:17Z"), instant)
    }

    @Test
    fun `format produces compact ISO string`() {
        val instant = Instant.parse("2026-02-27T07:45:17Z")
        assertEquals("20260227T074517Z", TimewarriorDateFormat.format(instant))
    }

    @Test
    fun `parse and format round-trip`() {
        val original = "20260101T120000Z"
        val instant = TimewarriorDateFormat.parse(original)
        assertEquals(original, TimewarriorDateFormat.format(instant))
    }

    @Test
    fun `parse midnight timestamp`() {
        val instant = TimewarriorDateFormat.parse("20260301T000000Z")
        assertEquals(Instant.parse("2026-03-01T00:00:00Z"), instant)
    }

    @Test
    fun `format strips fractional seconds`() {
        // Instant.parse may produce fractional seconds internally — format should strip them
        val instant = Instant.parse("2026-02-27T07:45:17.123Z")
        val formatted = TimewarriorDateFormat.format(instant)
        assertFalse(formatted.contains("."), "Should not contain fractional seconds")
        assertTrue(formatted.endsWith("Z"))
    }

    // --- Interval.isActive ---

    @Test
    fun `isActive is true when end is null`() {
        val interval = Interval(
            id = 1,
            start = Instant.parse("2026-02-27T08:00:00Z"),
            end = null,
            tags = listOf("coding")
        )
        assertTrue(interval.isActive)
    }

    @Test
    fun `isActive is false when end is set`() {
        val interval = Interval(
            id = 1,
            start = Instant.parse("2026-02-27T08:00:00Z"),
            end = Instant.parse("2026-02-27T09:30:00Z"),
            tags = listOf("coding")
        )
        assertFalse(interval.isActive)
    }

    // --- Interval.durationFormatted ---

    @Test
    fun `durationFormatted sub-hour`() {
        val interval = Interval(
            id = 1,
            start = Instant.parse("2026-02-27T08:00:00Z"),
            end = Instant.parse("2026-02-27T08:45:00Z")
        )
        assertEquals("45m", interval.durationFormatted)
    }

    @Test
    fun `durationFormatted exact hour`() {
        val interval = Interval(
            id = 1,
            start = Instant.parse("2026-02-27T08:00:00Z"),
            end = Instant.parse("2026-02-27T10:00:00Z")
        )
        assertEquals("2h", interval.durationFormatted)
    }

    @Test
    fun `durationFormatted multi-hour with minutes`() {
        val interval = Interval(
            id = 1,
            start = Instant.parse("2026-02-27T08:00:00Z"),
            end = Instant.parse("2026-02-27T10:15:00Z")
        )
        assertEquals("2h 15m", interval.durationFormatted)
    }

    // --- JSON serialization ---

    @Test
    fun `deserialize closed interval`() {
        val jsonStr = """{"id":1,"start":"20260227T080000Z","end":"20260227T093000Z","tags":["coding","review"]}"""
        val interval = json.decodeFromString<Interval>(jsonStr)
        assertEquals(1, interval.id)
        assertEquals(listOf("coding", "review"), interval.tags)
        assertFalse(interval.isActive)
    }

    @Test
    fun `deserialize active interval without end`() {
        val jsonStr = """{"id":2,"start":"20260227T080000Z","tags":["meeting"]}"""
        val interval = json.decodeFromString<Interval>(jsonStr)
        assertTrue(interval.isActive)
        assertNull(interval.end)
        assertEquals(listOf("meeting"), interval.tags)
    }

    @Test
    fun `deserialize interval with annotation`() {
        val jsonStr = """{"id":3,"start":"20260227T080000Z","end":"20260227T090000Z","tags":["dev"],"annotation":"Fixed bug #42"}"""
        val interval = json.decodeFromString<Interval>(jsonStr)
        assertEquals("Fixed bug #42", interval.annotation)
    }

    @Test
    fun `serialize and deserialize round-trip`() {
        val interval = Interval(
            id = 5,
            start = Instant.parse("2026-02-27T08:00:00Z"),
            end = Instant.parse("2026-02-27T09:00:00Z"),
            tags = listOf("work"),
            annotation = "Sprint planning"
        )
        val serialized = json.encodeToString(Interval.serializer(), interval)
        val deserialized = json.decodeFromString<Interval>(serialized)
        assertEquals(interval, deserialized)
    }
}
