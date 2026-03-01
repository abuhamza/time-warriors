package com.timewgui.domain.repository

import com.timewgui.domain.model.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

open class TaskRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val storageFile: File
        get() = File(System.getProperty("user.home"), ".config/timewgui/tasks.json")

    open fun load(): List<Task> {
        val file = storageFile
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Task>>(file.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    open fun save(tasks: List<Task>) {
        val file = storageFile
        file.parentFile.mkdirs()
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        try {
            tempFile.writeText(json.encodeToString(tasks))
            tempFile.renameTo(file)
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    fun generateTag(title: String): String {
        val slug = title
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(50)
        return "task:$slug"
    }
}
