package com.timewgui.domain

import com.timewgui.domain.model.RecurrenceEndType
import com.timewgui.domain.model.RecurrenceFrequency
import com.timewgui.domain.model.Task
import com.timewgui.domain.model.TaskStatus
import java.time.LocalDate
import java.time.ZoneId

object RecurrenceEngine {

    fun shouldGenerateForDate(template: Task, date: LocalDate, generatedCount: Int): Boolean {
        val rule = template.recurrenceRule ?: return false

        val dayOfWeek = date.dayOfWeek.value // 1=Mon..7=Sun
        val passesFrequency = when (rule.frequency) {
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKDAYS -> dayOfWeek in 1..5
            RecurrenceFrequency.WEEKLY -> dayOfWeek in rule.daysOfWeek
        }
        if (!passesFrequency) return false

        return when (rule.endType) {
            RecurrenceEndType.FOREVER -> true
            RecurrenceEndType.UNTIL_DATE -> {
                val endMillis = rule.endDate ?: return true
                startOfDay(date) <= endMillis
            }
            RecurrenceEndType.AFTER_OCCURRENCES -> {
                val max = rule.maxOccurrences ?: return true
                generatedCount < max
            }
        }
    }

    fun generateInstance(template: Task, date: LocalDate): Task = template.copy(
        id = java.util.UUID.randomUUID().toString(),
        status = TaskStatus.TODO,
        completedAt = null,
        recurrenceRule = null,
        recurrenceTemplateId = template.id,
        scheduledDate = startOfDay(date),
        createdAt = System.currentTimeMillis(),
    )

    fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
