package com.rokid.relayhud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TerminalPresentationTest {
    private val en = stringsForLocale(Locale.US)
    private val zh = stringsForLocale(Locale.forLanguageTag("zh-Hans-CN"))

    @Test fun approvedToolNamesAreNeverAbbreviated() {
        assertEquals("CODEX", displayToolName("codex"))
        assertEquals("CLAUDE", displayToolName("claude-code"))
        assertEquals("OPENCODE", displayToolName("OpenCode"))
    }

    @Test fun terminalHeaderContainsFullContextAndLocalizedStatus() {
        val state = HudState().apply {
            terminal = AoeTerminalSnapshot("1", "Code-Review2", "codex", "running", "", 0)
            mode = HudMode.AOE_TERMINAL
        }

        assertEquals("CODEX / Code-Review2 / Running", headerTextFor(state, en))
        assertEquals("CODEX / Code-Review2 / 运行", headerTextFor(state, zh))
    }

    @Test fun pageActionsUseCompactBracketedBbsCommands() {
        val state = HudState()
        assertEquals("[ENTER] Open", actionDockTextFor(state, en))
        state.selectedSessionIndex = -1
        assertEquals("[ENTER] New", actionDockTextFor(state, en))
        state.mode = HudMode.AOE_TERMINAL
        assertEquals("↑↓ Scroll  [ENTER] Reply  [BACK] Sessions", actionDockTextFor(state, en))
        state.mode = HudMode.AOE_TEXT_INPUT
        assertEquals("[ENTER] Send", actionDockTextFor(state, en))
    }

    @Test fun terminalFooterExplainsNativePromptNavigation() {
        val state = HudState().apply {
            mode = HudMode.AOE_TERMINAL
            terminal = AoeTerminalSnapshot(
                "1", "security-review", "claude", "idle",
                "❯ 1. Merge\n  2. Wait\nEnter to select · ↑/↓ to navigate", 3,
            )
        }
        assertEquals("↑↓ Select  [ENTER] Confirm  [BACK] Sessions", actionDockTextFor(state, en))
    }

    @Test fun replyAndNewSessionMenusUseBackForCancelInsteadOfCancelRows() {
        assertEquals(listOf(en.voiceDictation, en.textKeyboard), replyMethodOptions(en))
        assertEquals(listOf("CLAUDE", "CODEX", "OPENCODE"), newSessionOptions())
    }

    @Test fun menuNavigationCannotMoveOntoRemovedCancelRow() {
        val state = HudState().apply { mode = HudMode.AOE_REPLY_MENU }
        repeat(4) { state.scroll(1) }
        assertEquals(1, state.replyMenuIndex)
        state.mode = HudMode.AOE_NEW_SESSION_MENU
        repeat(4) { state.scroll(1) }
        assertEquals(2, state.newSessionIndex)
    }

    @Test fun newSessionHeaderUsesSelectedToolNotStaleTerminalContext() {
        val state = HudState().apply {
            terminal = AoeTerminalSnapshot("1", "Old Session", "claude", "running", "", 0)
            mode = HudMode.AOE_NEW_SESSION_MENU
            newSessionIndex = 2
        }
        assertEquals("OPENCODE / New session", headerTextFor(state, en))
        assertEquals("OPENCODE / 新建会话", headerTextFor(state, zh))
    }

    @Test fun dictationReviewDefaultsToContinueAndKeepsSegmentActionsExplicit() {
        assertEquals(listOf(en.send, en.continueAction, en.redo, en.cancelSegment), dictationReviewActions(en))
        assertEquals(1, DEFAULT_DICTATION_REVIEW_FOCUS)
    }

    @Test fun approved480x640RegionsNeverOverlap() {
        listOf(false, true).forEach { reviewFooter ->
            val bounds = terminalRegionBounds(reviewFooter = reviewFooter)
            assertTrue(bounds.header.last < bounds.body.first)
            assertTrue(bounds.body.last < bounds.footer.first)
            assertEquals(36, bounds.header.first)
            assertEquals(615, bounds.footer.last)
        }
    }
}
