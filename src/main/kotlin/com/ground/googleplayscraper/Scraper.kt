package com.ground.googleplayscraper

import com.ground.googleplayscraper.internal.HttpClient
import com.ground.googleplayscraper.internal.endpoints.AppEndpoint
import com.ground.googleplayscraper.internal.endpoints.ListEndpoint
import com.ground.googleplayscraper.internal.endpoints.PermissionsEndpoint
import com.ground.googleplayscraper.internal.endpoints.ReviewsEndpoint
import com.ground.googleplayscraper.internal.endpoints.SearchEndpoint
import com.ground.googleplayscraper.model.AppDetails
import com.ground.googleplayscraper.model.AppSummary
import com.ground.googleplayscraper.model.Permission
import com.ground.googleplayscraper.model.ReviewsResponse
import com.ground.googleplayscraper.model.SearchResult

/**
 * Public entry point of the library, the Kotlin counterpart of the JS
 * `google-play-scraper` module.
 *
 * ```kotlin
 * val scraper = Scraper()
 * val app = scraper.app("com.google.android.apps.maps", lang = "en", country = "us")
 * ```
 *
 * Every method performs a blocking HTTP call; call it from a background
 * thread/coroutine (`Dispatchers.IO`) on Android.
 */
class Scraper internal constructor(private val http: HttpClient) {

    constructor() : this(HttpClient())

    /** Full details of a single app. */
    fun app(
        appId: String,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY
    ): AppDetails = AppEndpoint(http).fetch(appId, lang, country)

    /** Search results for [term], or the next page when [nextToken] is given. */
    fun search(
        term: String,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY,
        price: GpConstants.Price? = null,
        nextToken: String? = null
    ): List<SearchResult> = SearchEndpoint(http).search(term, lang, country, price, nextToken)

    /** Reviews of an app, newest first by default. */
    fun reviews(
        appId: String,
        sort: GpConstants.Sort = GpConstants.Sort.NEWEST,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY,
        num: Int = 150,
        paginate: Boolean = false,
        nextPaginationToken: String? = null,
        stars: Int? = null,
        sentiment: GpConstants.Sentiment? = null
    ): ReviewsResponse = ReviewsEndpoint(http).reviews(
        appId = appId,
        sort = sort,
        lang = lang,
        country = country,
        num = num,
        paginate = paginate,
        nextPaginationToken = nextPaginationToken,
        stars = stars,
        sentiment = sentiment
    )

    /** Permissions declared by an app. */
    fun permissions(
        appId: String,
        short: Boolean = false,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY
    ): List<Permission> = PermissionsEndpoint(http).permissions(appId, short, lang, country)

    /** Chart listing (top free / paid / grossing) of an optional category. */
    fun list(
        collection: GpConstants.Collection = GpConstants.Collection.TOP_FREE,
        category: GpConstants.Category? = null,
        age: GpConstants.Age? = null,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY,
        num: Int? = 1000
    ): List<AppSummary> = ListEndpoint(http).list(collection, category, age, lang, country, num)

    /** Next page of a chart requested with a cluster token. */
    fun listNext(
        nextToken: String,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY,
        num: Int? = 1000
    ): List<AppSummary> = ListEndpoint(http).list(
        collection = GpConstants.Collection.TOP_FREE,
        category = null,
        age = null,
        lang = lang,
        country = country,
        num = num,
        nextToken = nextToken
    )
}
