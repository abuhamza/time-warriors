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
)
