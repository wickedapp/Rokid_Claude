package com.rokid.relayhud

import org.junit.Assert.assertEquals
import org.junit.Test

class AoeHudNavigationTest {
    private fun session(id: String, title: String, group: String) = AoeSessionSummary(
        id = id,
        title = title,
        tool = if (title.contains("Claude")) "claude" else "codex",
        group = group,
        status = "idle",
        path = "/tmp/$id",
        hasTerminal = true,
        unread = false,
        age = "1m",
        lastAccessedAt = "",
    )

    @Test fun groupedDisplayOrderKeepsEveryValetaxSessionContiguousAndReachable() {
        val input = listOf(
            session("v1", "Valetax One", "Valetax"),
            session("g1", "GGB", "GGB-Client"),
            session("v2", "Valetax Two", "Valetax"),
            session("h1", "Halley", "Halley-Codex"),
            session("v3", "Valetax Three", "Valetax"),
        )

        assertEquals(
            listOf("Valetax One", "Valetax Two", "Valetax Three", "GGB", "Halley"),
            groupSessionsForDisplay(input).map { it.title },
        )
    }

    @Test fun terminalStartsAtBottomAfterWrapping() {
        val content = (1..80).joinToString("\n") { "line $it" }
        assertEquals(44, terminalBottomStart(content, visibleCount = 36))
    }

    @Test fun shortTerminalStartsAtTop() {
        assertEquals(0, terminalBottomStart("one\ntwo", visibleCount = 36))
    }
}
