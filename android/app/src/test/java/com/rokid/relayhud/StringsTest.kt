package com.rokid.relayhud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsTest {
    @Test fun zhAndEnDiffer() {
        assertEquals("就绪", strings("zh").ready)
        assertEquals("Ready", strings("en").ready)
        assertEquals("Confirm", strings("en").confirm)
        assertEquals("Select model", strings("en").selectModel)
    }
    @Test fun unknownLangFallsBackZh() {
        assertEquals("就绪", strings("fr").ready)
    }
    @Test fun newSessionMatch() {
        assertTrue(matchesNewSession("new session", "en"))
        assertTrue(matchesNewSession("新会话", "zh"))
        assertTrue(!matchesNewSession("hello", "en"))
    }
    @Test fun exitMatch() {
        assertTrue(matchesExit("quit", "en"))
        assertTrue(matchesExit("退出", "zh"))
        assertTrue(!matchesExit("continue", "en"))
    }
}
