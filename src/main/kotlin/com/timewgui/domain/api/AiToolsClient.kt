package com.timewgui.domain.api

import com.timewgui.domain.model.SummaryRequest
import com.timewgui.domain.model.SummaryResponse
import com.timewgui.domain.model.GeneratedTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

open class AiToolsClient(
    private val baseUrl: String,
    private val token: String
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    open suspend fun generateSummary(request: SummaryRequest): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val requestBody = json.encodeToString(SummaryRequest.serializer(), request)
                val response = post("/api/ai/summary", requestBody)
                val parsed = json.decodeFromString<SummaryResponse>(response)
                parsed.summary
            }
        }

    open suspend fun generateTasks(text: String, existingTags: List<String>): Result<List<GeneratedTask>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val requestBody = json.encodeToString(
                    GenerateTasksRequestBody.serializer(),
                    GenerateTasksRequestBody(text, existingTags)
                )
                val response = post("/api/ai/generate-tasks", requestBody)
                val parsed = json.decodeFromString<GenerateTasksResponseBody>(response)
                parsed.tasks
            }
        }

    private fun post(path: String, body: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", "application/json")
            .header("Authorization", token)
            .timeout(java.time.Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("API returned ${response.statusCode()}: ${response.body()}")
        }
        return response.body()
    }
}

@Serializable
private data class GenerateTasksRequestBody(
    val text: String,
    val existingTags: List<String> = emptyList()
)

@Serializable
private data class GenerateTasksResponseBody(
    val tasks: List<GeneratedTask> = emptyList()
)
