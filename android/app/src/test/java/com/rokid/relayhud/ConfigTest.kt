package com.rokid.relayhud

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigTest {
    @Test fun nullOrBlankJson_returnsLocalDefault() {
        val c = parseConfig(null)
        assertEquals("ws://localhost:8788", c.serverUrl)
        assertEquals("", c.token)
        assertEquals(DEFAULT_CONFIG, parseConfig("   "))
    }

    @Test fun parsesServerUrlAndToken() {
        val c = parseConfig("""{"serverUrl":"wss://abc.ngrok-free.dev","token":"s3cret"}""")
        assertEquals("wss://abc.ngrok-free.dev", c.serverUrl)
        assertEquals("s3cret", c.token)
    }

    @Test fun malformedJson_fallsBackToDefault() {
        assertEquals(DEFAULT_CONFIG, parseConfig("not json {"))
    }

    @Test fun parsesLang() {
        assertEquals("en", parseConfig("""{"serverUrl":"ws://x","token":"","lang":"en"}""").lang)
    }

    @Test fun defaultsLangToZh() {
        assertEquals("zh", parseConfig("""{"serverUrl":"ws://x","token":""}""").lang)
    }

    @Test fun buildWsUrl_noToken_returnsHostUnchanged() {
        assertEquals("ws://localhost:8788", buildWsUrl("ws://localhost:8788", ""))
    }

    @Test fun buildWsUrl_withToken_appendsEncodedQuery() {
        assertEquals(
            "wss://abc.ngrok-free.dev/?token=a%2Bb",
            buildWsUrl("wss://abc.ngrok-free.dev", "a+b"),
        )
    }
}
