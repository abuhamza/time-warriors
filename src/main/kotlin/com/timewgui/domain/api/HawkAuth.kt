package com.timewgui.domain.api

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HawkAuth {

    fun generateHeader(
        id: String,
        key: String,
        method: String,
        path: String,
        host: String,
        port: Int,
        contentType: String = "",
        payload: String = ""
    ): String {
        val ts = System.currentTimeMillis() / 1000
        val nonce = generateNonce()
        val payloadHash = if (payload.isNotEmpty()) {
            hashPayload(contentType, payload)
        } else ""

        val normalized = buildNormalizedString(
            ts = ts,
            nonce = nonce,
            method = method.uppercase(),
            path = path,
            host = host,
            port = port,
            hash = payloadHash
        )

        val mac = computeMac(key, normalized)

        return buildString {
            append("Hawk id=\"$id\"")
            append(", ts=\"$ts\"")
            append(", nonce=\"$nonce\"")
            if (payloadHash.isNotEmpty()) {
                append(", hash=\"$payloadHash\"")
            }
            append(", mac=\"$mac\"")
        }
    }

    private fun buildNormalizedString(
        ts: Long,
        nonce: String,
        method: String,
        path: String,
        host: String,
        port: Int,
        hash: String
    ): String = buildString {
        append("hawk.1.header\n")
        append("$ts\n")
        append("$nonce\n")
        append("$method\n")
        append("$path\n")
        append("${host.lowercase()}\n")
        append("$port\n")
        append("$hash\n")
        append("\n")
    }

    private fun hashPayload(contentType: String, payload: String): String {
        val normalized = "hawk.1.payload\n$contentType\n$payload\n"
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }

    private fun computeMac(key: String, normalized: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        val result = mac.doFinal(normalized.toByteArray())
        return Base64.getEncoder().encodeToString(result)
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(6)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
