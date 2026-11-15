package com.ground.googleplayscraper.internal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Everything the Play Store embeds in an app/details page lives inside
 * `AF_initDataCallback` script blocks, keyed by names such as `ds:5`.
 *
 * [ScriptData] turns the raw HTML into a map from those keys to their parsed
 * JSON values, plus the `AF_dataServiceRequests` payload used by some views
 * (similar apps clusters).
 */
object ScriptData {

    private val SCRIPT_REGEX = Regex(">AF_initDataCallback[\\s\\S]*?</script")
    private val KEY_REGEX = Regex("(ds:.*?)'")
    private val VALUE_REGEX = Regex("data:([\\s\\S]*?),\\s*sideChannel:")
    private val SERVICE_REQUESTS_REGEX =
        Regex("; var AF_dataServiceRequests[\\s\\S]*?; var AF_initDataChunkQueue")
    private val SERVICE_VALUES_REGEX = Regex("\\{'ds:[\\s\\S]*}}")

    /** Parses the `AF_initDataCallback` blocks of a Play Store HTML page. */
    fun parse(html: String): ParsedData {
        val data = LinkedHashMap<String, JsonElement>()
        SCRIPT_REGEX.findAll(html).forEach { match ->
            val block = match.value
            val key = KEY_REGEX.find(block)?.groupValues?.get(1) ?: return@forEach
            val value = VALUE_REGEX.find(block)?.groupValues?.get(1) ?: return@forEach
            val parsed = runCatching { LenientJson.parseToJsonElement(value.trim().trimEnd(',', ';', '\n')) }
                .getOrNull() ?: return@forEach
            data[key] = parsed
        }
        return ParsedData(data, parseServiceRequests(html))
    }

    /** Parses the `AF_dataServiceRequests` variable embedded in the page, if any. */
    fun parseServiceRequests(html: String): Map<String, String> {
        val match = SERVICE_REQUESTS_REGEX.find(html) ?: return emptyMap()
        val body = SERVICE_VALUES_REGEX.find(match.value)?.value ?: return emptyMap()
        return parseJsObject(body)
    }

    /**
     * The service-request object is JavaScript (single quotes, unquoted keys),
     * so it cannot go through a JSON parser. Only its `id` values are needed.
     */
    private fun parseJsObject(raw: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val entry = Regex("'?(ds:[^':]+)'?\\s*:\\s*\\{[^}]*?id\\s*:\\s*'([^']+)'").findAll(raw)
        entry.forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }
        return result
    }
}

/** Result of parsing a Play Store page: the `ds:*` blocks and the service-request ids. */
class ParsedData(
    private val blocks: Map<String, JsonElement>,
    private val serviceRequests: Map<String, String>
) {

    val isEmpty: Boolean get() = blocks.isEmpty()

    operator fun get(key: String): JsonElement? = blocks[key]

    /** Raw block map, mostly useful for debugging. */
    val all: Map<String, JsonElement> get() = blocks

    /**
     * Returns the block whose `AF_dataServiceRequests` entry has the given id,
     * mirroring `extractDataWithServiceRequestId` of the JS implementation.
     */
    fun blockForServiceRequest(id: String): JsonElement? {
        val key = serviceRequests.entries.firstOrNull { it.value == id }?.key ?: return null
        return blocks[key]
    }
}

/** JSON parser tuned for the (slightly non-standard) payloads Google Play ships. */
internal object LenientJson {
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowTrailingComma = true
    }

    fun parseToJsonElement(source: String): JsonElement = json.parseToJsonElement(source)
}

/**
 * Path navigation over [JsonElement]s, replacing the `R.path` helper of the JS
 * implementation. String indices are accepted for objects, integers for arrays.
 */
internal object Paths {

    fun path(root: JsonElement?, vararg path: Any): JsonElement? {
        var current: JsonElement? = root
        for (part in path) {
            current = when (val node = current) {
                null, JsonNull -> return null
                is JsonArray -> {
                    val index = (part as? Int) ?: (part as? String)?.toIntOrNull() ?: return null
                    if (index < 0 || index >= node.size) return null
                    node[index]
                }
                is JsonObject -> {
                    val key = part.toString()
                    node[key]
                }
                else -> return null
            }
        }
        return current
    }

    fun string(root: JsonElement?, vararg path: Any): String? {
        val value = path(root, *path) as? JsonPrimitive ?: return null
        return if (value is JsonNull) null else value.contentOrNull()
    }

    fun long(root: JsonElement?, vararg path: Any): Long? =
        (path(root, *path) as? JsonPrimitive)?.contentOrNull()?.toLongOrNull()

    fun int(root: JsonElement?, vararg path: Any): Int? =
        (path(root, *path) as? JsonPrimitive)?.contentOrNull()?.toIntOrNull()

    fun double(root: JsonElement?, vararg path: Any): Double? =
        (path(root, *path) as? JsonPrimitive)?.contentOrNull()?.toDoubleOrNull()

    fun boolean(root: JsonElement?, vararg path: Any): Boolean? =
        (path(root, *path) as? JsonPrimitive)?.contentOrNull()?.toBooleanStrictOrNull()

    fun array(root: JsonElement?, vararg path: Any): JsonArray? =
        path(root, *path) as? JsonArray

    private fun JsonPrimitive.contentOrNull(): String? =
        if (this is JsonNull) null else content
}
