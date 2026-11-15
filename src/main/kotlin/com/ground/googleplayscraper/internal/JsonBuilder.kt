package com.ground.googleplayscraper.internal

import com.ground.googleplayscraper.GpConstants
import kotlinx.serialization.json.JsonPrimitive

/**
 * Builds the JSON payloads sent to `batchexecute`.
 *
 * The JS library builds those with `JSON.stringify`; doing the same here keeps
 * the request bodies comparable and avoids a custom encoder for the sparse
 * arrays Play expects.
 */
internal object JsonBuilder {

    /** Quotes a string the way `JSON.stringify` does. */
    fun quote(value: String): String = JsonPrimitive(value).toString()

    /**
     * Payload of the `oCPfdb` review request:
     * `[null, [2, <sort>, <paging>, null, <filters>], [<appId>, 7]]`.
     */
    fun buildReviewRequest(
        appId: String,
        sort: GpConstants.Sort,
        token: String?,
        stars: Int?,
        sentiment: GpConstants.Sentiment?
    ): String {
        val paging = if (token != null) "[150,null,${quote(token)}]" else "[150]"
        val filters = buildString {
            append("[null,")
            append(stars?.toString() ?: "null")
            append(",null,null,null,null,")
            append(sentiment?.raw ?: "null")
            append(",null,null]")
        }
        return "[null,[2,${sort.raw},$paging,null,$filters],[${quote(appId)},7]]"
    }

    /** Payload of the `xdSrCf` permissions request: `[[null,[<appId>,7],[]]]`. */
    fun buildPermissionsRequest(appId: String): String =
        "[[null,[${quote(appId)},7],[]]]"

    /** Payload of the `ceV5Cb` similar-apps request. */
    fun buildSimilarRequest(appId: String): String =
        "[[null,[${quote(appId)},7],[]]]"
}
