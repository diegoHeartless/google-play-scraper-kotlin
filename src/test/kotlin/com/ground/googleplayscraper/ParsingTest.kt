package com.ground.googleplayscraper

import com.ground.googleplayscraper.internal.MappingHelpers
import com.ground.googleplayscraper.internal.Paths
import com.ground.googleplayscraper.internal.ScriptData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParsingTest {

    @Test
    fun `dot paths navigate arrays and objects`() {
        // Real pages embed the block twice per script tag: a single-line first
        // copy without the data field, then the full one with data/sideChannel.
        val root = ScriptData.parse(
            """
            <script nonce="x">AF_initDataCallback({key: 'ds:3', isError:  false , hash: '1', sideChannel: {}});</script>
            <script nonce="x">AF_initDataCallback({key: 'ds:3', hash: '1', data:[0,[{"a":[1,2,3]}],null], sideChannel: {}});</script>
            """.trimIndent()
        )
        val block = root["ds:3"]
        assertNotNull(block)
        // data:[0,[{"a":[1,2,3]}],null] -> ds:3 is [0, [{a:[1,2,3]}], null]
        assertEquals(3, Paths.int(block, 1, 0, "a", 2))
        assertEquals(2, Paths.long(block, 1, 0, "a", 1))
        assertNull(Paths.string(block, 9, 9))
        assertNull(Paths.int(block, 1, 0, "a", 7))
    }

    @Test
    fun `description html becomes plain text`() {
        val text = MappingHelpers.descriptionText("Line 1<br>Line 2 &amp; more</p><b>bold</b>")
        assertEquals("Line 1\nLine 2 & more\nbold", text)
    }

    @Test
    fun `encode query value escapes like encodeURIComponent`() {
        assertEquals("caf%C3%A9%20bar", MappingHelpers.encodeQueryValue("café bar"))
        assertEquals("a-b_c.d~e", MappingHelpers.encodeQueryValue("a-b_c.d~e"))
    }
}
