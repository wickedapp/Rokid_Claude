package com.rokid.relayhud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DictationListeningPage(
    tool: String,
    session: String,
    committed: List<String>,
    transcribing: Boolean = false,
    s: Strings,
    connected: Boolean,
) {
    TerminalScaffold(
        header = "${displayToolName(tool)} / $session / ${if (transcribing) s.transcribing else s.listening}",
        connected = connected,
        actionLabel = if (transcribing) "[ ${s.transcribing} ]" else "[ ENTER ${s.stopSegment} ]",
        actionFocused = false,
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "${s.sendPreview} · ${committed.size}",
                style = TerminalTokens.MutedText,
                modifier = Modifier.fillMaxWidth().terminalBottomDivider(),
            )
            committed.forEachIndexed { index, segment ->
                Text(
                    "${(index + 1).toString().padStart(2, '0')} $segment",
                    style = TerminalTokens.Text,
                    modifier = Modifier.padding(top = TerminalTokens.Space4),
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    if (transcribing) s.transcribing else "● ${s.listening}…",
                    style = if (transcribing) TerminalTokens.MutedText else TerminalTokens.Text,
                )
            }
        }
    }
}

@Composable
fun DictationReviewPage(
    tool: String,
    session: String,
    committed: List<String>,
    candidate: String,
    s: Strings,
    connected: Boolean,
    focusedAction: Int = DEFAULT_DICTATION_REVIEW_FOCUS,
) {
    val actions = dictationReviewActions(s)
    TerminalScaffold(
        header = "${displayToolName(tool)} / $session / ${s.dictationReview}",
        connected = connected,
        actionLabel = "",
        actionContent = { ReviewActionDock(actions, focusedAction) },
    ) {
        TranscriptPanel(committedSegments = committed, candidate = candidate, s = s)
    }
}
