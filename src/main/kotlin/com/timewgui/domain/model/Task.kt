package com.timewgui.domain.model

import kotlinx.serialization.Serializable

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    ARCHIVED,
}

@Serializable
data class Task(
    val id: String,
    val title: String,
    val tag: String,
    val contextTags: List<String> = emptyList(),
    val status: TaskStatus = TaskStatus.TODO,
    val note: String = "",
    val createdAt: Long,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val recurrenceRule: RecurrenceRule? = null,       // non-null → this IS a template
    val recurrenceTemplateId: String? = null,         // non-null → this IS an instance
    val scheduledDate: Long? = null,                  // start-of-day epoch millis for instance
)
