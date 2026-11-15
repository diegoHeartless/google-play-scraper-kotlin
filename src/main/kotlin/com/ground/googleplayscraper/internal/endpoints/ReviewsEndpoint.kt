package com.ground.googleplayscraper.internal.endpoints

import com.ground.googleplayscraper.BASE_URL
import com.ground.googleplayscraper.DEFAULT_COUNTRY
import com.ground.googleplayscraper.DEFAULT_LANG
import com.ground.googleplayscraper.GpConstants
import com.ground.googleplayscraper.internal.BatchExecute
import com.ground.googleplayscraper.internal.HttpClient
import com.ground.googleplayscraper.internal.JsonBuilder
import com.ground.googleplayscraper.internal.MappingHelpers
import com.ground.googleplayscraper.internal.Paths
import com.ground.googleplayscraper.model.Review
import com.ground.googleplayscraper.model.ReviewCriteria
import com.ground.googleplayscraper.model.ReviewsResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * Port of `lib/reviews.js`: fetches reviews through the `oCPfdb` batchexecute
 * call, optionally walking the pagination tokens until [num] reviews are
 * collected.
 */
internal class ReviewsEndpoint(private val http: HttpClient) {

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
    ): ReviewsResponse {
        validate(appId, stars, sentiment)

        val accumulated = mutableListOf<Review>()
        var token: String? = nextPaginationToken?.takeIf { it.isNotEmpty() }

        while (true) {
            val page = requestPage(appId, sort, lang, country, token, stars, sentiment)
                ?: return ReviewsResponse(accumulated, null)

            accumulated += page.first
            token = page.second

            val enough = accumulated.size >= num
            if (enough || token.isNullOrEmpty()) {
                return ReviewsResponse(
                    data = if (accumulated.size >= num) accumulated.take(num) else accumulated,
                    nextPaginationToken = token
                )
            }
            // A non paginating call returns one page and the token for the next one.
            if (!paginate) {
                return ReviewsResponse(accumulated, token)
            }
        }
    }

    private fun requestPage(
        appId: String,
        sort: GpConstants.Sort,
        lang: String,
        country: String,
        token: String?,
        stars: Int?,
        sentiment: GpConstants.Sentiment?
    ): Pair<List<Review>, String?>? {
        val payload = JsonBuilder.buildReviewRequest(appId, sort, token, stars, sentiment)
        val response = BatchExecute.callRaw(http, RPC_ID, payload, lang, country) ?: return null
        val data = BatchExecute.extractPayload(response) ?: return null
        val reviews = Paths.array(data, 0)?.map { mapReview(it, appId) } ?: emptyList()
        val nextToken = Paths.string(data, 1, 1)
        return reviews to nextToken
    }

    companion object {

        private const val RPC_ID = "oCPfdb"

        fun validate(appId: String, stars: Int?, sentiment: GpConstants.Sentiment?) {
            require(appId.isNotBlank()) { "appId missing" }
            if (stars != null && stars !in 1..5) {
                throw IllegalArgumentException("Invalid stars $stars, expected a number from 1 to 5")
            }
            if (stars != null && sentiment != null) {
                throw IllegalArgumentException("Use either stars or sentiment, not both")
            }
        }

        /** Maps a single review entry. Exposed for testing. */
        fun mapReview(entry: JsonElement, appId: String): Review {
            val id = Paths.string(entry, 0)
            val score = Paths.int(entry, 2)
            return Review(
                id = id,
                userName = Paths.string(entry, 1, 0),
                userImage = Paths.string(entry, 1, 1, 3, 2),
                date = MappingHelpers.generateDate(Paths.array(entry, 5)),
                score = score,
                scoreText = score?.toString(),
                url = "$BASE_URL/store/apps/details?id=$appId&reviewId=$id",
                title = null,
                text = Paths.string(entry, 4),
                replyDate = MappingHelpers.generateDate(Paths.array(entry, 7, 2)),
                replyText = Paths.string(entry, 7, 1),
                version = Paths.string(entry, 10),
                thumbsUp = Paths.long(entry, 6),
                criterias = mapCriterias(Paths.array(entry, 12, 0))
            )
        }

        private fun mapCriterias(criterias: JsonArray?): List<ReviewCriteria> {
            if (criterias == null) return emptyList()
            return criterias.mapNotNull { criteria ->
                val name = Paths.string(criteria, 0) ?: return@mapNotNull null
                ReviewCriteria(
                    criteria = name,
                    rating = Paths.int(criteria, 1, 0)
                )
            }
        }
    }
}
