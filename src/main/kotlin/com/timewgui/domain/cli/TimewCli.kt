package com.timewgui.domain.cli

import com.timewgui.domain.model.Interval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * CLI integration service that wraps all `timew` commands.
 * This is the ONLY way the app communicates with Timewarrior.
 */
open class TimewCli(
    private val timewCommand: String = "timew",
    private val workingDirectory: File? = null
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Executes a timew command and captures stdout/stderr.
     */
    private suspend fun execute(vararg args: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(listOf(timewCommand) + args)
                .redirectErrorStream(true)
                .directory(workingDirectory)
                .start()

            val stdout = BufferedReader(InputStreamReader(process.inputStream))
                .use { it.readText() }

            val exitCode = process.waitFor(30, TimeUnit.SECONDS)
            if (exitCode) {
                Result.success(stdout.trim())
            } else {
                Result.failure(
                    TimewException(
                        "timew ${args.joinToString(" ")} failed with exit code $exitCode",
                        stdout.trim()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                TimewException(
                    "Failed to execute timew: ${e.message}",
                    null,
                    e
                )
            )
        }
    }

    suspend fun start(tags: List<String> = emptyList(), startTime: String? = null): Result<Unit> {
        val args = mutableListOf<String>()
        startTime?.let { args.add(it) }
        args.addAll(tags)
        return execute("start", *args.toTypedArray()).map { }
    }

    suspend fun stop(): Result<Unit> = execute("stop").map { }

    suspend fun cancel(): Result<Unit> = execute("cancel").map { }

    suspend fun continueTracking(id: Int? = null): Result<Unit> {
        val args = if (id != null) listOf("@$id") else emptyList()
        return execute("continue", *args.toTypedArray()).map { }
    }

    suspend fun track(startTime: String, endTime: String, tags: List<String>): Result<Unit> {
        val args = mutableListOf(startTime, "-", endTime)
        args.addAll(tags)
        return execute("track", *args.toTypedArray()).map { }
    }

    suspend fun deleteInterval(id: Int): Result<Unit> =
        execute("delete", "@$id").map { }

    suspend fun modifyStart(id: Int, newTime: String): Result<Unit> =
        execute("modify", "start", "@$id", newTime).map { }

    suspend fun modifyEnd(id: Int, newTime: String): Result<Unit> =
        execute("modify", "end", "@$id", newTime).map { }

    suspend fun moveInterval(id: Int, newStart: String): Result<Unit> =
        execute("move", "@$id", newStart).map { }

    suspend fun lengthen(id: Int, duration: String): Result<Unit> =
        execute("lengthen", "@$id", duration).map { }

    suspend fun shorten(id: Int, duration: String): Result<Unit> =
        execute("shorten", "@$id", duration).map { }

    suspend fun splitInterval(id: Int): Result<Unit> =
        execute("split", "@$id").map { }

    suspend fun joinIntervals(id1: Int, id2: Int): Result<Unit> =
        execute("join", "@$id1", "@$id2").map { }

    suspend fun addTags(id: Int, tags: List<String>): Result<Unit> {
        if (tags.isEmpty()) return Result.success(Unit)
        val args = listOf("@$id") + tags
        return execute("tag", *args.toTypedArray()).map { }
    }

    suspend fun removeTags(id: Int, tags: List<String>): Result<Unit> {
        if (tags.isEmpty()) return Result.success(Unit)
        val args = listOf("@$id") + tags
        return execute("untag", *args.toTypedArray()).map { }
    }

    suspend fun replaceTags(id: Int, tags: List<String>): Result<Unit> {
        val args = listOf("@$id") + tags
        return execute("retag", *args.toTypedArray()).map { }
    }

    suspend fun annotate(id: Int, annotation: String): Result<Unit> =
        execute("annotate", "@$id", annotation).map { }

    open suspend fun exportIntervals(range: String? = null, tags: List<String> = emptyList()): Result<List<Interval>> =
        withContext(Dispatchers.IO) {
            val args = mutableListOf<String>()
            range?.let { args.addAll(it.split(" ")) }
            args.addAll(tags)
            execute("export", *args.toTypedArray())
                .mapCatching { output ->
                    val trimmed = output.trim()
                    if (trimmed.isBlank() || trimmed == "[]") {
                        emptyList()
                    } else {
                        json.decodeFromString<List<Interval>>(trimmed)
                    }
                }
        }

    suspend fun getActiveTags(): Result<List<String>> =
        getActiveInterval().map { interval ->
            interval?.tags ?: emptyList()
        }

    suspend fun undo(): Result<Unit> = execute("undo").map { }

    suspend fun getActiveInterval(): Result<Interval?> =
        exportIntervals().map { intervals ->
            intervals.firstOrNull { it.isActive }
        }
}

/**
 * Exception thrown when a timew command fails.
 */
class TimewException(
    message: String,
    val output: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
