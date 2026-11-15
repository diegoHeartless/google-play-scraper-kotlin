package com.ground.googleplayscraper.model

/**
 * Rating distribution of an app, as shown on its Play Store page.
 */
data class Histogram(
    val one: Long = 0,
    val two: Long = 0,
    val three: Long = 0,
    val four: Long = 0,
    val five: Long = 0
) {
    operator fun get(star: Int): Long = when (star) {
        1 -> one
        2 -> two
        3 -> three
        4 -> four
        5 -> five
        else -> throw IllegalArgumentException("star must be between 1 and 5")
    }
}

/** A Play Store category an app belongs to. */
data class Category(
    val name: String?,
    val id: String?
)

/** A single review shown on the app details page ("comments"). */
data class Comment(
    val userName: String?,
    val userImage: String?,
    val score: Int?,
    val text: String?,
    val date: String? // ISO-8601
)

/** A "What's new" review-like entry, kept for parity with the JS library. */
data class AppFeature(
    val title: String?,
    val description: String?
)

/**
 * Every field Google Play exposes on an app details page.
 */
data class AppDetails(
    val appId: String?,
    val url: String?,
    val title: String?,
    val description: String?,
    val descriptionHTML: String?,
    val summary: String?,
    val installs: String?,
    val minInstalls: Long?,
    val maxInstalls: Long?,
    val score: Double?,
    val scoreText: String?,
    val ratings: Long?,
    val reviews: Long?,
    val histogram: Histogram?,
    val price: Double?,
    val originalPrice: Double?,
    val discountEndDate: Long?,
    val free: Boolean?,
    val currency: String?,
    val priceText: String?,
    val available: Boolean?,
    val offersIAP: Boolean?,
    val IAPRange: String?,
    val androidVersion: String?,
    val androidVersionText: String?,
    val androidMaxVersion: String?,
    val developer: String?,
    val developerId: String?,
    val developerEmail: String?,
    val developerWebsite: String?,
    val developerAddress: String?,
    val developerLegalName: String?,
    val developerLegalEmail: String?,
    val developerLegalAddress: String?,
    val developerLegalPhoneNumber: String?,
    val developerInternalID: String?,
    val privacyPolicy: String?,
    val genre: String?,
    val genreId: String?,
    val categories: List<Category> = emptyList(),
    val icon: String?,
    val headerImage: String?,
    val screenshots: List<String> = emptyList(),
    val video: String?,
    val videoImage: String?,
    val previewVideo: String?,
    val contentRating: String?,
    val contentRatingDescription: String?,
    val adSupported: Boolean?,
    val released: String?,
    val updated: Long?, // epoch millis
    val version: String?,
    val recentChanges: String?,
    val comments: List<Comment> = emptyList(),
    val preregister: Boolean?,
    val earlyAccessEnabled: Boolean?,
    val isAvailableInPlayPass: Boolean?
)

/**
 * The compact representation of an app used by search, chart and developer
 * listings.
 */
data class AppSummary(
    val appId: String?,
    val url: String?,
    val title: String?,
    val icon: String?,
    val developer: String?,
    val developerId: String?,
    val currency: String?,
    val price: Double?,
    val free: Boolean?,
    val summary: String?,
    val scoreText: String?,
    val score: Double?
)

/** One entry of the app search results. */
data class SearchResult(
    val appId: String?,
    val url: String?,
    val title: String?,
    val icon: String?,
    val developer: String?,
    val developerId: String?,
    val currency: String?,
    val price: Double?,
    val free: Boolean?,
    val summary: String?,
    val scoreText: String?,
    val score: Double?
)

/** A user review, as returned by the scraper's review call. */
data class Review(
    val id: String?,
    val userName: String?,
    val userImage: String?,
    val date: String?, // ISO-8601
    val score: Int?,
    val scoreText: String?,
    val url: String?,
    val title: String?,
    val text: String?,
    val replyDate: String?, // ISO-8601
    val replyText: String?,
    val version: String?,
    val thumbsUp: Long?,
    val criterias: List<ReviewCriteria> = emptyList()
)

/** Rating of a single review criterion (e.g. "Gameplay"). */
data class ReviewCriteria(
    val criteria: String?,
    val rating: Int?
)

/** Page of reviews plus the token needed to fetch the next one. */
data class ReviewsResponse(
    val data: List<Review>,
    val nextPaginationToken: String?
)

/** A permission requested by an app. */
data class Permission(
    val permission: String?,
    val type: Int?
)

/** An entry of the data safety section (data shared or collected). */
data class DataSafetyEntry(
    val data: String?,
    val optional: Boolean,
    val purpose: String?,
    val type: String?
)

/** A security practice declared in the data safety section. */
data class SecurityPractice(
    val practice: String?,
    val description: String?
)

/** Content of the "Data safety" tab of an app. */
data class DataSafety(
    val sharedData: List<DataSafetyEntry> = emptyList(),
    val collectedData: List<DataSafetyEntry> = emptyList(),
    val securityPractices: List<SecurityPractice> = emptyList(),
    val privacyPolicyUrl: String?
)
