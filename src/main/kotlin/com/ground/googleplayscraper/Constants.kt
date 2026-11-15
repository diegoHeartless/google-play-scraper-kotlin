package com.ground.googleplayscraper

/**
 * Base Play Store URL. Every request built by the library is relative to it.
 */
const val BASE_URL: String = "https://play.google.com"

/** Default language used when none is supplied (matches the original JS library). */
const val DEFAULT_LANG: String = "en"

/** Default country (gl) used when none is supplied. */
const val DEFAULT_COUNTRY: String = "us"

/**
 * Constants mirroring the original `google-play-scraper` (JS) constants object.
 */
object GpConstants {

    /** Chart collections accepted by [Scraper.list]. */
    enum class Collection(val raw: String) {
        TOP_FREE("TOP_FREE"),
        TOP_PAID("TOP_PAID"),
        GROSSING("GROSSING");

        override fun toString(): String = raw
    }

    /** Google Play categories accepted by [Scraper.list]. */
    enum class Category(val raw: String) {
        APPLICATION("APPLICATION"),
        ANDROID_WEAR("ANDROID_WEAR"),
        ART_AND_DESIGN("ART_AND_DESIGN"),
        AUTO_AND_VEHICLES("AUTO_AND_VEHICLES"),
        BEAUTY("BEAUTY"),
        BOOKS_AND_REFERENCE("BOOKS_AND_REFERENCE"),
        BUSINESS("BUSINESS"),
        COMICS("COMICS"),
        COMMUNICATION("COMMUNICATION"),
        DATING("DATING"),
        EDUCATION("EDUCATION"),
        ENTERTAINMENT("ENTERTAINMENT"),
        EVENTS("EVENTS"),
        FINANCE("FINANCE"),
        FOOD_AND_DRINK("FOOD_AND_DRINK"),
        HEALTH_AND_FITNESS("HEALTH_AND_FITNESS"),
        HOUSE_AND_HOME("HOUSE_AND_HOME"),
        LIBRARIES_AND_DEMO("LIBRARIES_AND_DEMO"),
        LIFESTYLE("LIFESTYLE"),
        MAPS_AND_NAVIGATION("MAPS_AND_NAVIGATION"),
        MEDICAL("MEDICAL"),
        MUSIC_AND_AUDIO("MUSIC_AND_AUDIO"),
        NEWS_AND_MAGAZINES("NEWS_AND_MAGAZINES"),
        PARENTING("PARENTING"),
        PERSONALIZATION("PERSONALIZATION"),
        PHOTOGRAPHY("PHOTOGRAPHY"),
        PRODUCTIVITY("PRODUCTIVITY"),
        SHOPPING("SHOPPING"),
        SOCIAL("SOCIAL"),
        SPORTS("SPORTS"),
        TOOLS("TOOLS"),
        TRAVEL_AND_LOCAL("TRAVEL_AND_LOCAL"),
        VIDEO_PLAYERS("VIDEO_PLAYERS"),
        WATCH_FACE("WATCH_FACE"),
        WEATHER("WEATHER"),
        GAME("GAME"),
        GAME_ACTION("GAME_ACTION"),
        GAME_ADVENTURE("GAME_ADVENTURE"),
        GAME_ARCADE("GAME_ARCADE"),
        GAME_BOARD("GAME_BOARD"),
        GAME_CARD("GAME_CARD"),
        GAME_CASINO("GAME_CASINO"),
        GAME_CASUAL("GAME_CASUAL"),
        GAME_EDUCATIONAL("GAME_EDUCATIONAL"),
        GAME_MUSIC("GAME_MUSIC"),
        GAME_PUZZLE("GAME_PUZZLE"),
        GAME_RACING("GAME_RACING"),
        GAME_ROLE_PLAYING("GAME_ROLE_PLAYING"),
        GAME_SIMULATION("GAME_SIMULATION"),
        GAME_SPORTS("GAME_SPORTS"),
        GAME_STRATEGY("GAME_STRATEGY"),
        GAME_TRIVIA("GAME_TRIVIA"),
        GAME_WORD("GAME_WORD"),
        FAMILY("FAMILY");

        override fun toString(): String = raw

        companion object {
            /** @throws IllegalArgumentException when [raw] is not a known category. */
            fun fromRaw(raw: String): Category =
                entries.firstOrNull { it.raw == raw }
                    ?: throw IllegalArgumentException("Invalid category $raw")
        }
    }

    /** Review sort orders understood by the batchexecute endpoint. */
    enum class Sort(val raw: Int) {
        NEWEST(2),
        RATING(3),
        HELPFULNESS(1)
    }

    /** Review sentiment filter. */
    enum class Sentiment(val raw: Int) {
        POSITIVE(1),
        CRITICAL(2)
    }

    /** Age ranges accepted by [Scraper.list]. */
    enum class Age(val raw: String) {
        FIVE_UNDER("AGE_RANGE1"),
        SIX_EIGHT("AGE_RANGE2"),
        NINE_UP("AGE_RANGE3")
    }

    /** Price filter accepted by [Scraper.search]. */
    enum class Price(val raw: Int) {
        ALL(0),
        FREE(1),
        PAID(2)
    }

    /** Android permission buckets returned by the permissions endpoint. */
    enum class PermissionType(val raw: Int) {
        COMMON(0),
        OTHER(1)
    }
}
