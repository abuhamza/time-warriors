package com.timewgui.domain.model

import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration

/**
 * Utility for parsing and formatting Timewarrior's compact ISO format: "YYYYMMDDTHHmmssZ"
 */
object TimewarriorDateFormat {
    private val compactFormat = kotlinx.datetime.format.DateTimeComponents.Format {
        byUnicodePattern("uuuuMMdd'T'HHmmssX")
    }

    /**
     * Parses a compact ISO string like "20260227T074517Z" to [Instant].
     */
    fun parse(input: String): Instant {
        val components = compactFormat.parse(input)
        return components.toInstantUsingOffset()
    }

    /**
     * Formats an [Instant] to the compact ISO string format.
     */
    fun format(instant: Instant): String {
        // Instant.toString() gives "2026-02-27T07:45:17.123Z" - convert to compact "20260227T074517Z"
        val s = instant.toString().replace("-", "").replace(":", "")
        val withoutFraction = s.substringBefore(".")
        return if (withoutFraction.endsWith("Z")) withoutFraction else "${withoutFraction}Z"
    }
}

/**
 * Custom serializer for [Instant] using Timewarrior's compact ISO format.
 */
object InstantTimewarriorSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(TimewarriorDateFormat.format(value))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return TimewarriorDateFormat.parse(decoder.decodeString())
    }
}

/**
 * Custom serializer for nullable [Instant] (used for optional `end` in active intervals).
 */
object InstantTimewarriorSerializerNullable : KSerializer<Instant?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant?", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(TimewarriorDateFormat.format(value))
        }
    }

    override fun deserialize(decoder: Decoder): Instant? {
        if (!decoder.decodeNotNullMark()) {
            decoder.decodeNull()
            return null
        }
        return TimewarriorDateFormat.parse(decoder.decodeString())
    }
}

/**
 * Data model for a Timewarrior interval, matching `timew export` JSON format.
 */
@Serializable
data class Interval(
    val id: Int,
    @Serializable(with = InstantTimewarriorSerializer::class)
    val start: Instant,
    @Serializable(with = InstantTimewarriorSerializerNullable::class)
    val end: Instant? = null,
    val tags: List<String> = emptyList(),
    val annotation: String? = null
) {
    /**
     * True if this interval is currently active (no end time).
     */
    val isActive: Boolean
        get() = end == null

    /**
     * Duration of the interval. For active intervals, computes from start to now.
     */
    val duration: Duration
        get() {
            val endInstant = end ?: Clock.System.now()
            return endInstant - start
        }

    /**
     * Human-readable formatted duration (e.g., "2h 15m").
     */
    val durationFormatted: String
        get() = formatDuration(duration)

    companion object {
        private fun formatDuration(duration: Duration): String {
            val totalMinutes = duration.inWholeMinutes
            if (totalMinutes < 60) return "${totalMinutes}m"
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
        }
    }
}
