package com.github.diegoheartless.gplayscraper.internal.endpoints

import com.github.diegoheartless.gplayscraper.BASE_URL
import com.github.diegoheartless.gplayscraper.DEFAULT_COUNTRY
import com.github.diegoheartless.gplayscraper.DEFAULT_LANG
import com.github.diegoheartless.gplayscraper.internal.HttpClient
import com.github.diegoheartless.gplayscraper.internal.MappingHelpers
import com.github.diegoheartless.gplayscraper.internal.ParsedData
import com.github.diegoheartless.gplayscraper.internal.Paths
import com.github.diegoheartless.gplayscraper.internal.ScriptData
import com.github.diegoheartless.gplayscraper.model.AppDetails
import com.github.diegoheartless.gplayscraper.model.Category
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Fetches the full app details page (`/store/apps/details`) and maps it to
 * [AppDetails]. Port of `lib/app.js`.
 */
internal class AppEndpoint(private val http: HttpClient) {

    fun fetch(
        appId: String,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY
    ): AppDetails {
        require(appId.isNotBlank()) { "appId missing" }
        val url = url(appId, lang, country)
        val html = http.get(url)
        val details = mapApp(ScriptData.parse(html))
        return details.copy(appId = appId, url = url)
    }

    companion object {

        private const val PLAYSTORE_URL = "$BASE_URL/store/apps/details"

        fun url(appId: String, lang: String = DEFAULT_LANG, country: String = DEFAULT_COUNTRY): String =
            HttpClient.url(
                PLAYSTORE_URL,
                linkedMapOf("id" to appId, "hl" to lang, "gl" to country)
            )

        /** Maps parsed page data to [AppDetails]. Exposed for testing. */
        fun mapApp(data: ParsedData): AppDetails {
            val ds5 = data["ds:5"]
            val search = Paths.path(ds5, 1, 2)

            val priceRaw = Paths.path(ds5, 1, 2, 57, 0, 0, 0, 0, 1, 0, 0)
            val originalPriceRaw = Paths.path(ds5, 1, 2, 57, 0, 0, 0, 0, 1, 1, 0)

            val androidVersion = pickAndroidVersion(ds5)
            val androidVersionText = androidVersion.takeUnless { it.isNullOrBlank() } ?: "Varies with device"

            val developerUrl = Paths.string(ds5, 1, 2, 68, 1, 4, 2)
            val developerId = developerUrl?.substringAfter("id=", "")

            val categories = MappingHelpers.extractCategories(Paths.path(search, 118))
                .ifEmpty {
                    listOf(
                        Category(
                            name = Paths.string(search, 79, 0, 0, 0),
                            id = Paths.string(search, 79, 0, 0, 2)
                        )
                    )
                }

            val screenshots = Paths.array(ds5, 1, 2, 78, 0)?.screenshotUrls() ?: emptyList()

            val version = Paths.string(ds5, 1, 2, 140, 0, 0, 0)
                ?: Paths.string(ds5, 1, 2, 141, 0, 0, 0)
                ?: "VARY"

            val updatedSeconds = Paths.long(ds5, 1, 2, 145, 0, 1, 0)
                ?: Paths.long(ds5, 1, 2, 146, 0, 1, 0)

            val androidMaxVersion = (Paths.string(ds5, 1, 2, 140, 1, 1, 0, 1, 1)
                ?: Paths.string(ds5, 1, 2, 141, 1, 1, 0, 1, 1))
                ?.let { MappingHelpers.normalizeAndroidVersion(it) }

            val descriptionHtml = MappingHelpers.descriptionHtmlLocalized(search)

            return AppDetails(
                appId = null,
                url = null,
                title = Paths.string(ds5, 1, 2, 0, 0),
                description = MappingHelpers.descriptionText(descriptionHtml),
                descriptionHTML = descriptionHtml,
                summary = Paths.string(ds5, 1, 2, 73, 0, 1),
                installs = Paths.string(ds5, 1, 2, 13, 0),
                minInstalls = Paths.long(ds5, 1, 2, 13, 1),
                maxInstalls = Paths.long(ds5, 1, 2, 13, 2),
                score = Paths.double(ds5, 1, 2, 51, 0, 1),
                scoreText = Paths.string(ds5, 1, 2, 51, 0, 0),
                ratings = Paths.long(ds5, 1, 2, 51, 2, 1),
                reviews = Paths.long(ds5, 1, 2, 51, 3, 1),
                histogram = MappingHelpers.buildHistogram(Paths.array(ds5, 1, 2, 51, 1)),
                price = priceRaw?.let { Paths.double(it, 0)?.div(1_000_000) } ?: 0.0,
                originalPrice = originalPriceRaw?.let { Paths.double(it, 0)?.div(1_000_000) },
                discountEndDate = Paths.long(ds5, 1, 2, 57, 0, 0, 0, 0, 14, 1),
                free = priceRaw?.let { Paths.double(it, 0) == 0.0 },
                currency = Paths.string(ds5, 1, 2, 57, 0, 0, 0, 0, 1, 0, 1),
                priceText = MappingHelpers.priceText(Paths.string(ds5, 1, 2, 57, 0, 0, 0, 0, 1, 0, 2)),
                available = Paths.path(ds5, 1, 2, 18, 0) != null,
                offersIAP = Paths.path(ds5, 1, 2, 19, 0) != null,
                IAPRange = Paths.string(ds5, 1, 2, 19, 0),
                androidVersion = MappingHelpers.normalizeAndroidVersion(androidVersion),
                androidVersionText = androidVersionText,
                androidMaxVersion = androidMaxVersion,
                developer = Paths.string(ds5, 1, 2, 68, 0),
                developerId = developerId,
                developerEmail = Paths.string(ds5, 1, 2, 69, 1, 0),
                developerWebsite = Paths.string(ds5, 1, 2, 69, 0, 5, 2),
                developerAddress = Paths.string(ds5, 1, 2, 69, 2, 0),
                developerLegalName = Paths.string(ds5, 1, 2, 69, 4, 0),
                developerLegalEmail = Paths.string(ds5, 1, 2, 69, 4, 1, 0),
                developerLegalAddress = Paths.string(ds5, 1, 2, 69, 4, 2, 0)?.replace("\n", ", "),
                developerLegalPhoneNumber = Paths.string(ds5, 1, 2, 69, 4, 3),
                developerInternalID = developerId,
                privacyPolicy = Paths.string(ds5, 1, 2, 99, 0, 5, 2),
                genre = Paths.string(ds5, 1, 2, 79, 0, 0, 0),
                genreId = Paths.string(ds5, 1, 2, 79, 0, 0, 2),
                categories = categories,
                icon = Paths.string(ds5, 1, 2, 95, 0, 3, 2),
                headerImage = Paths.string(ds5, 1, 2, 96, 0, 3, 2),
                screenshots = screenshots,
                video = Paths.string(ds5, 1, 2, 100, 0, 0, 3, 2),
                videoImage = Paths.string(ds5, 1, 2, 100, 1, 0, 3, 2),
                previewVideo = Paths.string(ds5, 1, 2, 100, 1, 2, 0, 2),
                contentRating = Paths.string(ds5, 1, 2, 9, 0),
                contentRatingDescription = Paths.string(ds5, 1, 2, 9, 2, 1),
                adSupported = Paths.path(ds5, 1, 2, 48) != null,
                released = Paths.string(ds5, 1, 2, 10, 0),
                updated = updatedSeconds?.times(1000),
                version = version,
                recentChanges = Paths.string(ds5, 1, 2, 144, 1, 1)
                    ?: Paths.string(ds5, 1, 2, 145, 1, 1),
                comments = MappingHelpers.extractComments(data),
                preregister = Paths.long(ds5, 1, 2, 18, 0) == 1L,
                earlyAccessEnabled = Paths.path(ds5, 1, 2, 18, 2) is JsonPrimitive,
                isAvailableInPlayPass = Paths.path(ds5, 1, 2, 62) != null
            )
        }

        private fun pickAndroidVersion(ds5: JsonElement?): String? =
            Paths.string(ds5, 1, 2, 140, 1, 1, 0, 0, 1)
                ?: Paths.string(ds5, 1, 2, 141, 1, 1, 0, 0, 1)

        /** Screen shots may be plain strings or nested arrays depending on the page version. */
        private fun JsonArray.screenshotUrls(): List<String> = mapNotNull { element ->
            Paths.string(element, 3, 2)
        }
    }
}
