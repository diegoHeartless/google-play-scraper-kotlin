package com.github.diegoheartless.gplayscraper.internal

import com.github.diegoheartless.gplayscraper.model.AppFeature
import com.github.diegoheartless.gplayscraper.model.Category
import com.github.diegoheartless.gplayscraper.model.Comment
import com.github.diegoheartless.gplayscraper.model.Histogram
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.net.URLEncoder
import java.time.Instant

/**
 * Pure helpers shared by the endpoint implementations. They mirror
 * `lib/utils/mappingHelpers.js` of the JS library one to one, so that both
 * libraries produce the same values for the same page.
 */
object MappingHelpers {

    /** Extracts the localized description, falling back to the original one. */
    fun descriptionHtmlLocalized(searchArray: JsonElement?): String? {
        val translation = Paths.string(searchArray, 12, 0, 0, 1)
        val original = Paths.string(searchArray, 72, 0, 1)
        return translation ?: original ?: ""
    }

    /**
     * Turns the description HTML into plain text, keeping line breaks.
     * <br>, <br/> and </p> become newlines, every other tag is dropped and the
     * handful of entities Play uses are decoded.
     */
    fun descriptionText(description: String?): String {
        if (description.isNullOrEmpty()) return ""
        val withBreaks = description
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<li>", RegexOption.IGNORE_CASE), "• ")
        return withBreaks
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }

    /** Returns `"Free"` when Play provides no price text. */
    fun priceText(priceText: String?): String = priceText?.takeIf { it.isNotBlank() } ?: "Free"

    /** Normalizes the required Android version (`"VARY"` when unknown). */
    fun normalizeAndroidVersion(androidVersionText: String?): String {
        if (androidVersionText.isNullOrBlank()) return "VARY"
        val number = androidVersionText.split(" ").first()
        val parsed = number.toDoubleOrNull()
        return if (parsed != null && parsed != 0.0) number else "VARY"
    }

    /** Builds the rating histogram out of the `[star, count]` pairs Play ships. */
    fun buildHistogram(container: JsonArray?): Histogram {
        if (container == null) return Histogram()
        fun value(star: Int): Long = Paths.long(container, star, 1) ?: 0L
        return Histogram(
            one = value(1),
            two = value(2),
            three = value(3),
            four = value(4),
            five = value(5)
        )
    }

    /**
     * Extracts the top comments of an app page. They migrate between `ds:8` and
     * `ds:9` from time to time, so both paths are inspected for the fields the
     * library relies on.
     */
    fun extractComments(data: ParsedData): List<Comment> {
        val reportRegex = Regex("""^[A-Za-z]{3} \d{1,2}, \d{4}$""")

        for (path in listOf("ds:8", "ds:9")) {
            val root = data[path] ?: continue
            val author = Paths.string(root, 0, 0, 1, 0)
            val version = Paths.string(root, 0, 0, 10)
            val date = Paths.string(root, 0, 0, 5, 0)
            if (author == null || version == null || date == null) continue

            val entries = Paths.array(root, 0) ?: continue
            return entries.take(5).mapNotNull { entry ->
                val block = Paths.path(entry, 4) ?: return@mapNotNull null
                val rawDate = Paths.string(block, 5, 0)
                Comment(
                    userName = Paths.string(block, 1, 0),
                    userImage = Paths.string(block, 1, 1, 3, 2),
                    score = Paths.int(block, 2),
                    text = Paths.string(block, 4),
                    date = parsePlayDate(rawDate)
                )
            }.filterNot { it.userName == null && it.text == null }
                .filter { it.date == null || !reportRegex.containsMatchIn(it.date) }
        }
        return emptyList()
    }

    /** Extracts the "recent changes" style features of the page. */
    fun extractFeatures(featuresArray: JsonElement?): List<AppFeature> {
        if (featuresArray == null) return emptyList()
        val features = Paths.array(featuresArray, 2) ?: return emptyList()
        return features.mapNotNull { feature ->
            AppFeature(
                title = Paths.string(feature, 0),
                description = Paths.string(feature, 1, 0, 0, 1)
            )
        }
    }

    /** Recursively collects the categories nested in the category block. */
    fun extractCategories(searchArray: JsonElement?, result: MutableList<Category> = mutableListOf()): List<Category> {
        val node = searchArray as? JsonArray ?: return result
        if (node.isEmpty()) return result

        if (node.size >= 4 && node[0] is JsonPrimitive) {
            result.add(Category(name = Paths.string(node, 0), id = Paths.string(node, 2)))
        } else {
            node.forEach { child -> extractCategories(child, result) }
        }
        return result.distinct()
    }

    /**
     * Parses the human readable date used in comments (`"Aug 12, 2023"`) into an
     * ISO-8601 instant. Returns null when the date cannot be understood.
     */
    fun parsePlayDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH)
        return runCatching {
            val date = java.time.LocalDate.parse(raw.trim(), formatter)
            date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toString()
        }.getOrNull()
    }

    /** Builds the epoch-millis string used by reviews, given Play's `[millis, nanos]` pair. */
    fun generateDate(dateArray: JsonArray?): String? {
        if (dateArray == null) return null
        val seconds = Paths.long(dateArray, 0) ?: return null
        val nanos = Paths.long(dateArray, 1) ?: 0L
        val millisLastDigits = nanos.toString().padStart(3, '0').take(3)
        val total = "$seconds$millisLastDigits".toLongOrNull() ?: return null
        return Instant.ofEpochMilli(total).toString()
    }

    /**
     * Percent-encodes a value exactly like JS `encodeURIComponent`, so that the
     * search URL is built identically to the original library.
     */
    fun encodeQueryValue(value: String): String = buildString {
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val code = byte.toInt() and 0xFF
            val char = code.toChar()
            val unreserved = (code < 128 && (char.isLetterOrDigit())) || char in "-_.!~*'()"
            if (unreserved) {
                append(char)
            } else {
                append('%')
                append("%02X".format(code))
            }
        }
    }
}
