package com.github.diegoheartless.gplayscraper.internal

import com.github.diegoheartless.gplayscraper.BASE_URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Minimal client for the Play Store `batchexecute` endpoint, the same call the
 * web frontend performs for permissions, reviews and similar apps.
 *
 * The response is a length-prefixed list of chunks; the payload of interest is
 * the third element of the inner array of the chunk carrying the rpc id.
 */
internal object BatchExecute {

    /** Performs the call and returns the parsed payload, or null when empty. */
    fun call(
        http: HttpClient,
        rpcId: String,
        payload: String,
        lang: String,
        country: String
    ): JsonElement? {
        val url = HttpClient.url(
            "$BASE_URL/_/PlayStoreUi/data/batchexecute",
            linkedMapOf(
                "rpcids" to "qnKhOb",
                "f.sid" to "-697906427155521722",
                "bl" to "boq_playuiserver_20190903.08_p0",
                "hl" to lang,
                "gl" to country,
                "authuser" to "",
                "soc-app" to "121",
                "soc-platform" to "1",
                "soc-device" to "1",
                "_reqid" to "1065213"
            )
        )
        val body = "[[[\"$rpcId\",${JsonPrimitive(payload).toString()},null,\"generic\"]]]"
        val response = http.post(url, mapOf("f.req" to body))
        return extractPayload(response)
    }

    /** Extracts the `payload` field of a batchexecute response. */
    fun extractPayload(response: String): JsonElement? {
        val body = response.substringAfter(")]}'", response).trim()
        if (body.isEmpty()) return null

        val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            val parsed = runCatching { Json.parseToJsonElement(line) }.getOrNull() ?: continue
            val chunks = parsed as? JsonArray ?: continue
            for (chunk in chunks) {
                val entry = chunk as? JsonArray ?: continue
                if (entry.size < 3) continue
                val data = entry[2]
                if (data is JsonNull) return null
                if (data is JsonPrimitive) {
                    return runCatching { Json.parseToJsonElement(data.content) }.getOrNull()
                }
                return data
            }
        }
        return null
    }
}
