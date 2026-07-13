package com.rokid.relayhud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictationDraftTest {
    @Test fun beginStartsAContinuousSessionWithAnEmptyPreview() {
        val draft = DictationDraft()
        draft.begin()
        assertTrue(draft.active)
        assertEquals(DictationPhase.LISTENING, draft.phase)
        assertEquals(emptyList<String>(), draft.committed.toList())
        assertEquals("", draft.candidate)
    }

    @Test fun transcriptMovesToReviewWithoutSending() {
        val draft = DictationDraft().apply { begin() }
        draft.receiveTranscript("第一段文字")
        assertEquals(DictationPhase.REVIEW, draft.phase)
        assertEquals("第一段文字", draft.candidate)
        assertEquals(emptyList<String>(), draft.committed.toList())
        assertEquals(DEFAULT_DICTATION_REVIEW_FOCUS, draft.focusedAction)
    }

    @Test fun continueCommitsCurrentSegmentAndListensForTheNext() {
        val draft = DictationDraft().apply { begin(); receiveTranscript("第一段") }
        draft.continueListening()
        assertEquals(listOf("第一段"), draft.committed.toList())
        assertEquals("", draft.candidate)
        assertEquals(DictationPhase.LISTENING, draft.phase)
    }

    @Test fun redoDiscardsOnlyCurrentCandidateAndKeepsPreview() {
        val draft = DictationDraft().apply {
            begin(); receiveTranscript("保留"); continueListening(); receiveTranscript("听错")
        }
        draft.redoCurrent()
        assertEquals(listOf("保留"), draft.committed.toList())
        assertEquals("", draft.candidate)
        assertEquals(DictationPhase.LISTENING, draft.phase)
    }

    @Test fun cancelSegmentNeverClearsCommittedPreview() {
        val draft = DictationDraft().apply {
            begin(); receiveTranscript("保留"); continueListening(); receiveTranscript("取消这一段")
        }
        draft.cancelCurrent()
        assertEquals(listOf("保留"), draft.committed.toList())
        assertEquals("", draft.candidate)
        assertEquals(DictationPhase.LISTENING, draft.phase)
    }

    @Test fun sendTextIncludesPreviewAndCurrentCandidate() {
        val draft = DictationDraft().apply {
            begin(); receiveTranscript("第一段"); continueListening(); receiveTranscript("第二段")
        }
        assertEquals("第一段\n第二段", draft.textToSend())
        draft.finish()
        assertFalse(draft.active)
        assertEquals(emptyList<String>(), draft.committed.toList())
    }

    @Test fun terminalRefreshCannotDismissAnActiveInputOverlay() {
        val overlayModes = listOf(
            HudMode.AOE_REPLY_MENU,
            HudMode.AOE_TEXT_INPUT,
            HudMode.AOE_NEW_SESSION_MENU,
            HudMode.AOE_DICTATION_LISTENING,
            HudMode.AOE_DICTATION_REVIEW,
        )
        overlayModes.forEach { overlayMode ->
            val state = HudState().apply {
                if (overlayMode in listOf(HudMode.AOE_DICTATION_LISTENING, HudMode.AOE_DICTATION_REVIEW)) {
                    dictation.begin()
                    if (overlayMode == HudMode.AOE_DICTATION_REVIEW) dictation.receiveTranscript("text")
                }
                mode = overlayMode
            }
            state.showTerminal(
                AoeTerminalSnapshot("1", "Code-Review2", "codex", "running", "new output", 1),
                stringsForLocale(java.util.Locale.US),
            )
            assertEquals(overlayMode, state.mode)
        }
    }

    @Test fun createdSessionTerminalCompletesNewSessionFlow() {
        val state = HudState().apply {
            mode = HudMode.AOE_NEW_SESSION_MENU
            creatingSession = true
        }
        state.showTerminal(
            AoeTerminalSnapshot("new", "Rokid-opencode", "opencode", "running", "ready", 1),
            stringsForLocale(java.util.Locale.US),
        )
        assertEquals(HudMode.AOE_TERMINAL, state.mode)
        assertFalse(state.creatingSession)
        assertEquals("new", state.activeSessionId)
    }

    @Test fun reviewFocusStaysWithinFourActions() {
        val draft = DictationDraft().apply { begin(); receiveTranscript("text") }
        repeat(8) { draft.moveFocus(1) }
        assertEquals(3, draft.focusedAction)
        repeat(8) { draft.moveFocus(-1) }
        assertEquals(0, draft.focusedAction)
    }
}
