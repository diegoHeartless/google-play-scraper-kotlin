package com.ground.googleplayscraper.internal.endpoints

import com.ground.googleplayscraper.BASE_URL
import com.ground.googleplayscraper.DEFAULT_COUNTRY
import com.ground.googleplayscraper.DEFAULT_LANG
import com.ground.googleplayscraper.GpConstants
import com.ground.googleplayscraper.internal.BatchExecute
import com.ground.googleplayscraper.internal.HttpClient
import com.ground.googleplayscraper.internal.Paths
import com.ground.googleplayscraper.internal.ScriptData
import com.ground.googleplayscraper.model.SearchResult

/**
 * Port of `lib/search.js`: searches the Play Store and, when [nextToken] is
 * supplied, fetches the next page of the same query.
 */
internal class SearchEndpoint(private val http: HttpClient) {

    fun search(
        term: String,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY,
        price: GpConstants.Price? = null,
        nextToken: String? = null
    ): List<SearchResult> {
        require(term.isNotBlank()) { "term missing" }

        val html = if (!nextToken.isNullOrEmpty()) {
            val payload = BatchExecute.call(
                http = http,
                rpcId = "qnKhOb",
                payload = "[[null,[[null,[null,$nextToken]],null,[null,null]]]",
                lang = lang,
                country = country
            )
            payload?.toString()?.let { unwrapHtml(it) }.orEmpty()
        } else {
            http.get(buildUrl(term, lang, country, price))
        }

        return AppSummaryMapper.extractApps(html).map { summary ->
            SearchResult(
                appId = summary.appId,
                url = AppSummaryMapper.appUrl(summary.appId, lang, country),
                title = summary.title,
                icon = summary.icon,
                developer = summary.developer,
                developerId = summary.developerId,
                currency = summary.currency,
                price = summary.price,
                free = summary.free,
                summary = summary.summary,
                scoreText = summary.scoreText,
                score = summary.score
            )
        }
    }

    companion object {

        /** Builds the URL of the first search page, including the optional price filter. */
        fun buildUrl(
            term: String,
            lang: String = DEFAULT_LANG,
            country: String = DEFAULT_COUNTRY,
            price: GpConstants.Price? = null
        ): String {
            val pricePart = when {
                price == null || price == GpConstants.Price.ALL -> ""
                price == GpConstants.Price.FREE -> "p1"
                else -> "p2"
            }
            return HttpClient.url(
                "$BASE_URL/store/search",
                linkedMapOf(
                    "c" to "apps",
                    "q" to AppSummaryMapper.encodeTerm(term),
                    "hl" to lang,
                    "gl" to country
                )
            ) + pricePart
        }

        /** Turns the JSON string carried by batchexecute into parsable HTML. */
        private fun unwrapHtml(payload: String): String =
            payload
                .removeSurrounding("\"")
                .replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\/", "/")

        /** Extracts the continuation token of a search page, for the next request. */
        fun nextToken(html: String): String? {
            val data = ScriptData.parse(html)
            return Paths.string(data["ds:3"], *AppSummaryMapper.SEARCH_TOKEN_PATH)
                ?: Paths.string(data["ds:2"], 0, 0, 0, 28, 0, 0, 1)
        }
    }
}
