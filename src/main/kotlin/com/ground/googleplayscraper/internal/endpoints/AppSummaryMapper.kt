package com.ground.googleplayscraper.internal.endpoints

import com.ground.googleplayscraper.BASE_URL
import com.ground.googleplayscraper.internal.HttpClient
import com.ground.googleplayscraper.internal.MappingHelpers
import com.ground.googleplayscraper.internal.Paths
import com.ground.googleplayscraper.internal.ScriptData
import com.ground.googleplayscraper.model.AppSummary
import kotlinx.serialization.json.JsonElement

/**
 * Maps the compact app entries that appear in search and chart listings.
 *
 * The Play Store keeps two layouts for those entries:
 *  - the now common web layout (`ds:3, 0, 1, 0, 21, 0`);
 *  - a legacy layout used for old `/store/search` responses.
 * Both are handled so that the library keeps working regardless of which one
 * the current response uses.
 */
internal object AppSummaryMapper {

    /** Apps block of the standard web layout. */
    const val WEB_APPS_PATH = "ds:3"
    val SEARCH_APPS_PATH = arrayOf<Any>(0, 1, 0, 21, 0)
    val SEARCH_TOKEN_PATH = arrayOf<Any>(0, 1, 0, 21, 1, 3, 1)

    fun mapWebEntry(entry: JsonElement): AppSummary {
        val developerUrl = Paths.string(entry, 14, 11, 0, 0, 1)
        val developerId = developerUrl?.substringAfter("?id=", "")?.substringBefore("&")?.takeIf { it.isNotEmpty() }
            ?: Paths.string(entry, 14, 11, 0, 0, 0)

        val priceRaw = Paths.path(entry, 8, 1, 0, 0)

        return AppSummary(
            appId = Paths.string(entry, 0, 0),
            url = Paths.string(entry, 10, 4, 2)?.let { BASE_URL + it },
            title = Paths.string(entry, 3),
            icon = Paths.string(entry, 1, 3, 2),
            developer = Paths.string(entry, 14, 0),
            developerId = developerId,
            currency = Paths.string(entry, 8, 1, 0, 1),
            price = priceRaw?.let { Paths.double(it, 0)?.div(1_000_000) },
            free = priceRaw?.let { Paths.double(it, 0) == 0.0 },
            summary = Paths.string(entry, 13, 1),
            scoreText = Paths.string(entry, 4, 0),
            score = Paths.double(entry, 4, 1)
        )
    }

    /** Legacy layout used by `/store/search` for old form factors. */
    fun mapLegacyEntry(entry: JsonElement): AppSummary {
        val priceRaw = Paths.path(entry, 8, 1, 0, 0)
        return AppSummary(
            appId = Paths.string(entry, 0, 0),
            url = Paths.string(entry, 10, 4, 2)?.let { BASE_URL + it },
            title = Paths.string(entry, 3),
            icon = Paths.string(entry, 1, 3, 2),
            developer = Paths.string(entry, 14),
            developerId = Paths.string(entry, 14, 11, 0, 0, 1)?.substringAfter("?id=", "")?.substringBefore("&"),
            currency = Paths.string(entry, 8, 1, 0, 1),
            price = priceRaw?.let { Paths.double(it, 0)?.div(1_000_000) },
            free = priceRaw?.let { Paths.double(it, 0) == 0.0 },
            summary = Paths.string(entry, 13, 1),
            scoreText = Paths.string(entry, 4, 0),
            score = Paths.double(entry, 4, 1)
        )
    }

    /** Searches the standard `21,0` slot and falls back to the legacy block. */
    fun extractApps(html: String): List<AppSummary> {
        val data = ScriptData.parse(html)

        val webApps = Paths.array(data["ds:3"], *SEARCH_APPS_PATH)
        if (webApps != null && webApps.isNotEmpty()) {
            return webApps.map { mapWebEntry(it) }
        }

        // Legacy layout wraps the entry in a one-element array.
        val legacy = Paths.array(data["ds:2"], 0, 0, 0, 28) ?: return emptyList()
        return legacy.mapNotNull { token -> Paths.path(token, 0)?.let { mapLegacyEntry(it) } }
    }

    /** Returns the continuation token of the current page, when more results exist. */
    fun extractToken(html: String): String? {
        val data = ScriptData.parse(html)
        return Paths.string(data["ds:3"], *SEARCH_TOKEN_PATH)
            ?: Paths.string(data["ds:2"], 0, 0, 0, 28, 0, 0, 1)
    }

    /** Builds the canonical Play Store app URL for an app id. */
    fun appUrl(appId: String?, lang: String, country: String): String =
        HttpClient.url(
            "$BASE_URL/store/apps/details",
            linkedMapOf("id" to (appId ?: ""), "hl" to lang, "gl" to country)
        )

    /** Fills in [AppSummary.url] using the request locale, matching the JS helper. */
    fun withUrl(app: AppSummary, lang: String, country: String): AppSummary =
        app.copy(url = appUrl(app.appId, lang, country))

    /** Encodes a search term for use inside the search URL path. */
    fun encodeTerm(term: String): String = MappingHelpers.encodeQueryValue(term)
}
