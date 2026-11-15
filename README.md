# google-play-scraper (Kotlin)

Kotlin/JVM port of the popular Node.js library
[`google-play-scraper`](https://github.com/facundoolano/google-play-scraper).

It scrapes the public Google Play website (no API key, no authentication) and
returns typed data classes instead of raw JSON.

## Features

| Method | JS counterpart | Description |
| --- | --- | --- |
| `app(appId, lang, country)` | `app()` | every field of the app details page |
| `search(term, price, nextToken)` | `search()` | search results, paged |
| `reviews(appId, sort, num, paginate, stars, sentiment)` | `reviews()` | reviews through `batchexecute` |
| `permissions(appId, short)` | `permissions()` | declared Android permissions |
| `list(collection, category, age)` | `list()` | top free / paid / grossing charts |
| `listNext(nextToken)` | `list()` | next page of a chart |

## Install

The library is a plain JVM artifact (`java-library`, Kotlin 1.9, JVM 17), so it
can be used from a server, a CLI or an Android app.

```kotlin
dependencies {
    implementation("com.ground:google-play-scraper-kotlin:1.0.0")
}
```

## Build

```bash
gradle build        # compiles the library, runs the unit tests, produces build/libs/*.jar
gradle publishToMavenLocal
```

The repository has no Gradle wrapper yet, so build with a local Gradle 8.7+
(`gradle wrapper` generates the wrapper if you want the usual `./gradlew`).

CI (`.github/workflows/build.yml`) builds and tests every push and pull request
against `main` on JDK 17 and uploads the jar plus the test report as artifacts.
The tests are offline: they cover the request payloads and the parsing helpers,
not the live Play Store.


## Usage

```kotlin
import com.ground.googleplayscraper.Scraper
import com.ground.googleplayscraper.GpConstants

val scraper = Scraper()

// app details
val maps = scraper.app("com.google.android.apps.maps", lang = "en", country = "us")
println("${maps.title} — ${maps.score} (${maps.installs})")
println(maps.screenshots.firstOrNull())
println(maps.histogram?.five)

// search
val results = scraper.search("notes", price = GpConstants.Price.FREE)
results.forEach { println("${it.appId} ${it.title}") }

// reviews
val reviews = scraper.reviews("com.google.android.apps.maps", num = 200, paginate = true)
reviews.data.forEach { println("${it.score} ${it.text}") }
val next = reviews.nextPaginationToken

// permissions
val permissions = scraper.permissions("com.google.android.apps.maps", short = true)

// charts
val top = scraper.list(
    collection = GpConstants.Collection.TOP_FREE,
    category = GpConstants.Category.GAME
)
```

All calls are blocking: on Android run them off the main thread, e.g.
`withContext(Dispatchers.IO) { scraper.app(id) }`.

### Options

- `lang` — `hl` query parameter, defaults to `en`;
- `country` — `gl` query parameter, defaults to `us`;
- `GpConstants.Collection`, `Category`, `Age`, `Sort`, `Sentiment`, `Price` —
  the same enumerations the JS library exposes as `collection`, `category`,
  `age`, `sort`, `sort`/`sentiment`, `price`.

## Models

Everything lives in `com.ground.googleplayscraper.model`:
`AppDetails`, `AppSummary`, `SearchResult`, `ReviewsResponse`, `Review`,
`ReviewCriteria`, `Permission`, `Category`, `Comment`, `Histogram`,
`DataSafety`, `DataSafetyEntry`, `SecurityPractice`.

## Layout

```
src/main/kotlin/com/ground/googleplayscraper/
├── Constants.kt          # base URL, defaults, GpConstants enums
├── Scraper.kt            # public facade
├── model/Models.kt       # data classes
└── internal/
    ├── HttpClient.kt     # OkHttp wrapper
    ├── ScriptData.kt     # AF_initDataCallback parsing + path navigation
    ├── BatchExecute.kt   # /_/PlayStoreUi/data/batchexecute client
    ├── JsonBuilder.kt    # request payloads
    ├── MappingHelpers.kt # JS mapping-helpers port
    └── endpoints/        # app, search, reviews, permissions, list
```

## Disclaimer

The library scrapes the public website. Google may change the page layout at any
time; the parsing paths are kept in one place per endpoint to make updating them
easy. Use it for personal and research purposes and respect Google's terms of
service.
