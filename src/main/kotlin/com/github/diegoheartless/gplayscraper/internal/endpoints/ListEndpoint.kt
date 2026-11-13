package com.github.diegoheartless.gplayscraper.internal.endpoints

import com.github.diegoheartless.gplayscraper.BASE_URL
import com.github.diegoheartless.gplayscraper.DEFAULT_COUNTRY
import com.github.diegoheartless.gplayscraper.DEFAULT_LANG
import com.github.diegoheartless.gplayscraper.GpConstants
import com.github.diegoheartless.gplayscraper.internal.HttpClient
import com.github.diegoheartless.gplayscraper.internal.Paths
import com.github.diegoheartless.gplayscraper.internal.ScriptData
import com.github.diegoheartless.gplayscraper.model.AppSummary

/**
 * Port of `lib/list.js`: fetches a chart (top free / paid / grossing) of a
 * category, optionally filtered by age range.
 */
internal class ListEndpoint(private val http: HttpClient) {

    fun list(
        collection: GpConstants.Collection = GpConstants.Collection.TOP_FREE,
        category: GpConstants.Category? = null,
        age: GpConstants.Age? = null,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY,
        num: Int? = 1000,
        nextToken: String? = null
    ): List<AppSummary> {
        val url = if (!nextToken.isNullOrEmpty()) {
            clusterUrl(nextToken, lang, country)
        } else {
            buildUrl(collection, category, age, lang, country)
        }

        val html = http.get(url)
        val data = ScriptData.parse(html)

        val apps = Paths.array(data["ds:3"], 0, 1, 0, 21, 0) ?: return emptyList()
        val mapped = apps.map { AppSummaryMapper.mapWebEntry(it) }

        return if (num != null && num > 0) {
            mapped.take(num).map { AppSummaryMapper.withUrl(it, lang, country) }
        } else {
            mapped.map { AppSummaryMapper.withUrl(it, lang, country) }
        }
    }

    companion object {

        fun buildUrl(
            collection: GpConstants.Collection = GpConstants.Collection.TOP_FREE,
            category: GpConstants.Category? = null,
            age: GpConstants.Age? = null,
            lang: String = DEFAULT_LANG,
            country: String = DEFAULT_COUNTRY
        ): String {
            val builder = StringBuilder("$BASE_URL/store/apps")
            if (category != null) builder.append("/category/").append(category.raw)
            builder.append("?hl=").append(lang).append("&gl=").append(country)
            if (age != null) {
                builder.append("&age=").append(age.raw)
            } else {
                builder.append("&collection=").append(collection.raw)
            }
            return builder.toString()
        }

        /** URL used for the `similar()` endpoint, kept here to share the constant. */
        fun clusterUrl(clusterId: String, lang: String = DEFAULT_LANG, country: String = DEFAULT_COUNTRY): String =
            HttpClient.url(
                "$BASE_URL/store/apps/collection/cluster",
                linkedMapOf("hl" to lang, "gl" to country, "id" to clusterId)
            )
    }
}
