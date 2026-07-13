package com.rokid.relayhud

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class DictationPhase { IDLE, LISTENING, TRANSCRIBING, REVIEW }

/** Observable segmented voice draft. Confirmed segments survive redo/cancel of the current segment. */
class DictationDraft {
    val committed = mutableStateListOf<String>()
    var candidate by mutableStateOf("")
        private set
    var phase by mutableStateOf(DictationPhase.IDLE)
        private set
    var focusedAction by mutableIntStateOf(DEFAULT_DICTATION_REVIEW_FOCUS)
        private set

    val active: Boolean get() = phase != DictationPhase.IDLE

    fun begin() {
        committed.clear()
        candidate = ""
        focusedAction = DEFAULT_DICTATION_REVIEW_FOCUS
        phase = DictationPhase.LISTENING
    }

    fun markTranscribing() {
        if (active) phase = DictationPhase.TRANSCRIBING
    }

    fun receiveTranscript(text: String) {
        if (!active) return
        candidate = text.trim()
        focusedAction = DEFAULT_DICTATION_REVIEW_FOCUS
        phase = if (candidate.isEmpty()) DictationPhase.LISTENING else DictationPhase.REVIEW
    }

    fun continueListening() {
        if (candidate.isNotBlank()) committed += candidate
        candidate = ""
        focusedAction = DEFAULT_DICTATION_REVIEW_FOCUS
        phase = DictationPhase.LISTENING
    }

    fun redoCurrent() {
        candidate = ""
        focusedAction = DEFAULT_DICTATION_REVIEW_FOCUS
        phase = DictationPhase.LISTENING
    }

    fun cancelCurrent() {
        candidate = ""
        focusedAction = DEFAULT_DICTATION_REVIEW_FOCUS
        phase = DictationPhase.LISTENING
    }

    fun moveFocus(delta: Int) {
        focusedAction = (focusedAction + delta).coerceIn(0, 3)
    }

    fun textToSend(): String = (committed + listOf(candidate))
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString("\n")

    fun finish() {
        committed.clear()
        candidate = ""
        focusedAction = DEFAULT_DICTATION_REVIEW_FOCUS
        phase = DictationPhase.IDLE
    }
}
