package com.timewgui.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GeneratedTask(
    val title: String,
    val tags: List<String> = emptyList()
)
