package com.timewgui.domain.api

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class HawkAuthTest {

    private val testId = "test-key-id"
    private val testKey = "test-secret-key"

    @Test
    fun `header starts with Hawk id`() {
        val header = HawkAuth.generateHeader(
            id = testId,
            key = testKey,
            method = "GET",
            path = "/api/v2/absences",
            host = "app.absence.io",
            port = 443
        )
        assertTrue(header.startsWith("Hawk id=\"$testId\""), "Header should start with Hawk id")
    }

    @Test
    fun `header with payload includes hash field`() {
        val header = HawkAuth.generateHeader(
            id = testId,
            key = testKey,
            method = "POST",
            path = "/api/v2/absences",
            host = "app.absence.io",
            port = 443,
            contentType = "application/json",
            payload = """{"skip":0,"limit":10}"""
        )
        assertTrue(header.contains("hash=\""), "Header with payload should include hash field")
    }

    @Test
    fun `header without payload omits hash field`() {
        val header = HawkAuth.generateHeader(
            id = testId,
            key = testKey,
            method = "GET",
            path = "/api/v2/absences",
            host = "app.absence.io",
            port = 443
        )
        assertFalse(header.contains("hash=\""), "Header without payload should omit hash field")
    }

    @Test
    fun `two calls produce different nonces`() {
        val header1 = HawkAuth.generateHeader(
            id = testId, key = testKey,
            method = "GET", path = "/api/v2/test",
            host = "app.absence.io", port = 443
        )
        val header2 = HawkAuth.generateHeader(
            id = testId, key = testKey,
            method = "GET", path = "/api/v2/test",
            host = "app.absence.io", port = 443
        )
        val nonce1 = Regex("""nonce="([^"]+)"""").find(header1)!!.groupValues[1]
        val nonce2 = Regex("""nonce="([^"]+)"""").find(header2)!!.groupValues[1]
        assertNotEquals(nonce1, nonce2, "Nonces should be unique per call")
    }
}
