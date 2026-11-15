package com.ground.googleplayscraper

import com.ground.googleplayscraper.internal.JsonBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonBuilderTest {

    @Test
    fun `permissions payload matches the JS library`() {
        assertEquals(
            """[[null,["com.example.app",7],[]]]""",
            JsonBuilder.buildPermissionsRequest("com.example.app")
        )
    }

    @Test
    fun `review payload uses the first page without a token`() {
        val payload = JsonBuilder.buildReviewRequest(
            appId = "com.example.app",
            sort = GpConstants.Sort.NEWEST,
            token = null,
            stars = null,
            sentiment = null
        )
        assertTrue(payload.startsWith("""[null,[2,2,[150],null,[null,null,null,null,null,null,null,null,null]]"""))
        assertTrue(payload.endsWith("""[com.example.app",7]]"""))
    }

    @Test
    fun `review payload carries the pagination token and filters`() {
        val payload = JsonBuilder.buildReviewRequest(
            appId = "com.example.app",
            sort = GpConstants.Sort.RATING,
            token = "TOKEN",
            stars = 4,
            sentiment = null
        )
        assertTrue(payload.contains("""[150,null,"TOKEN"]"""))
        assertTrue(payload.contains("[null,4,null,null,null,null,null,null,null]"))
    }

    @Test
    fun `quote escapes like JSON stringify`() {
        assertEquals(""""a\"b"""", JsonBuilder.quote("a\"b"))
    }
}
