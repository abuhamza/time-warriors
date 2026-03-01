package com.timewgui.domain.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AbsenceIoClient(
    private val keyId: String,
    private val keySecret: String
) {
    private val httpClient = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val host = "app.absence.io"
    private val baseUrl = "https://$host"

    suspend fun fetchAbsences(from: LocalDate, to: LocalDate): Result<List<AbsenceEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // The API key ID doubles as the user's ID in absence.io.
                // Filter by assignedToId to get only this user's absences (not the whole company).
                val body = """
                    {
                      "skip": 0,
                      "limit": 1000,
                      "filter": {
                        "assignedToId": "$keyId",
                        "start": { "${'$'}gte": "${from}T00:00:00.000Z" },
                        "end": { "${'$'}lte": "${to}T23:59:59.999Z" }
                      },
                      "relations": ["reasonId"]
                    }
                """.trimIndent()

                val parsed = post("/api/v2/absences", body)
                val response = json.decodeFromString<AbsenceResponse>(parsed)
                // Filter: approved + not countsAsWork (excludes "mobile Arbeit" / home office)
                response.data.filter { entry ->
                    entry.forwardHistory.any { it.action == "approved" } &&
                        entry.reason?.countsAsWork != true
                }
            }
        }

    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = """{"skip": 0, "limit": 1, "filter": {"assignedToId": "$keyId"}}"""
            val parsed = post("/api/v2/absences", body)
            val response = json.decodeFromString<AbsenceResponse>(parsed)
            if (response.totalCount < 0) error("Unexpected response")
        }
    }

    private fun post(path: String, body: String): String {
        val contentType = "application/json"
        val authHeader = HawkAuth.generateHeader(
            id = keyId,
            key = keySecret,
            method = "POST",
            path = path,
            host = host,
            port = 443,
            contentType = contentType,
            payload = body
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", contentType)
            .header("Authorization", authHeader)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("absence.io API returned ${response.statusCode()}: ${response.body()}")
        }
        return response.body()
    }
}

@Serializable
data class AbsenceResponse(
    val data: List<AbsenceEntry> = emptyList(),
    val totalCount: Int = 0
)

@Serializable
data class AbsenceEntry(
    @SerialName("_id") val id: String = "",
    val start: String = "",
    val end: String = "",
    val assignedToId: String = "",
    val reason: AbsenceReason? = null,
    val reasonId: String? = null,
    val daysCount: Double = 0.0,
    val forwardHistory: List<ForwardHistoryEntry> = emptyList()
)

@Serializable
data class ForwardHistoryEntry(
    val action: String = "",
    val admin: String = "",
    val date: String = ""
)

@Serializable
data class AbsenceReason(
    @SerialName("_id") val id: String = "",
    val name: String = "",
    val countsAsWork: Boolean = false
)
