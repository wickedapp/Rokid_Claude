package com.rokid.relayhud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val Green = TerminalTokens.Foreground
val DimGreen = TerminalTokens.Muted
val FocusBg = TerminalTokens.FocusBackground

data class HudLine(val text: String, val color: Color = Green)
enum class HudMode {
    AOE_SESSIONS,
    AOE_TERMINAL,
    AOE_REPLY_MENU,
    AOE_TEXT_INPUT,
    AOE_NEW_SESSION_MENU,
    AOE_DICTATION_LISTENING,
    AOE_DICTATION_REVIEW,
}

data class PermissionPrompt(
    val id: String,
    val summary: String,
    val options: List<String>,
    val allowKey: String,
    val highlight: Int,
    val secondsLeft: Int,
    val title: String,
)

class HudState {
    val lines = mutableStateListOf<HudLine>()
    val aoeSessions = mutableStateListOf<AoeSessionSummary>()
    var mode by mutableStateOf(HudMode.AOE_SESSIONS)
    var selectedSessionIndex by mutableStateOf(0)
    var activeSessionId by mutableStateOf<String?>(null)
    var terminal by mutableStateOf<AoeTerminalSnapshot?>(null)
    var replyMenuIndex by mutableStateOf(0)
    var newSessionIndex by mutableStateOf(0)
    var creatingSession by mutableStateOf(false)
    var textInput by mutableStateOf("")
    var terminalScroll by mutableIntStateOf(0)
    var terminalFollowBottom by mutableStateOf(true)
        private set
    var status by mutableStateOf("")
    val toolIndex = mutableMapOf<String, Int>()
    var recording by mutableStateOf(false)
    val dictation = DictationDraft()
    var choice by mutableStateOf<PermissionPrompt?>(null)
    var blanked by mutableStateOf(false)
    var statusline by mutableStateOf("")
    var scrollTick by mutableStateOf(0)
        private set
    var scrollDir = 0
        private set

    private var lastWasText = false

    fun add(text: String, color: Color = Green) {
        lines.add(HudLine(text, color))
        lastWasText = false
    }

    fun addText(text: String) {
        text.split("\n").forEachIndexed { index, part ->
            if (index == 0 && lastWasText && lines.isNotEmpty()) {
                val last = lines.lastIndex
                lines[last] = lines[last].copy(text = lines[last].text + part)
            } else {
                lines.add(HudLine(part, Green))
            }
        }
        lastWasText = true
    }

    fun setAoeSessions(items: List<AoeSessionSummary>, s: Strings) {
        val ordered = groupSessionsForDisplay(items)
        aoeSessions.clear()
        aoeSessions.addAll(ordered)
        if (selectedSessionIndex >= aoeSessions.size) {
            selectedSessionIndex = (aoeSessions.size - 1).coerceAtLeast(0)
        }
        if (mode !in setOf(
                HudMode.AOE_TERMINAL,
                HudMode.AOE_REPLY_MENU,
                HudMode.AOE_TEXT_INPUT,
                HudMode.AOE_DICTATION_LISTENING,
                HudMode.AOE_DICTATION_REVIEW,
            )
        ) {
            mode = HudMode.AOE_SESSIONS
        }
        val claude = ordered.count { it.tool.contains("claude", true) }
        val codex = ordered.count { it.tool.contains("codex", true) }
        val opencode = ordered.count { it.tool.contains("open", true) }
        status = "${s.recent} · CLAUDE $claude · CODEX $codex · OPENCODE $opencode"
    }

    fun moveSession(delta: Int) {
        val min = -1
        val max = (aoeSessions.size - 1).coerceAtLeast(min)
        selectedSessionIndex = (selectedSessionIndex + delta).coerceIn(min, max)
    }

    fun showTerminal(snapshot: AoeTerminalSnapshot, s: Strings) {
        val overlayMode = mode in setOf(
            HudMode.AOE_REPLY_MENU,
            HudMode.AOE_TEXT_INPUT,
            HudMode.AOE_NEW_SESSION_MENU,
            HudMode.AOE_DICTATION_LISTENING,
            HudMode.AOE_DICTATION_REVIEW,
        )
        if (overlayMode && !(mode == HudMode.AOE_NEW_SESSION_MENU && creatingSession)) {
            terminal = snapshot
            activeSessionId = snapshot.id
            return
        }
        creatingSession = false
        val sameSession = terminal?.id == snapshot.id
        if (!sameSession || mode != HudMode.AOE_TERMINAL) terminalFollowBottom = true
        terminal = snapshot
        activeSessionId = snapshot.id
        val bottom = terminalBottomStart(snapshot.content)
        terminalScroll = if (terminalFollowBottom) bottom else terminalScroll.coerceIn(0, bottom)
        mode = HudMode.AOE_TERMINAL
        status = "${displayToolName(snapshot.tool)} / ${snapshot.title} / ${localizedAoeStatus(snapshot.status, s)}"
    }

    fun terminalLooksInteractive(): Boolean {
        val text = terminal?.content?.takeLast(4000)?.lowercase() ?: return false
        val explicitPrompt = listOf(
            "use arrow", "arrow keys", "choose an option", "select an option",
            "enter to select", "↑/↓ to navigate", "type something",
        ).any(text::contains)
        val selectionCursor = text.lineSequence().any { it.trimStart().startsWith("❯ ") }
        return explicitPrompt || selectionCursor
    }

    fun terminalAtBottom(): Boolean {
        val content = terminal?.content ?: return true
        return terminalScroll >= terminalBottomStart(content) - 1
    }

    fun scroll(dir: Int) {
        when (mode) {
            HudMode.AOE_TERMINAL -> {
                val bottom = terminalBottomStart(terminal?.content.orEmpty())
                terminalScroll = (terminalScroll + dir).coerceIn(0, bottom)
                terminalFollowBottom = terminalScroll >= bottom
            }
            HudMode.AOE_REPLY_MENU -> replyMenuIndex = (replyMenuIndex + dir).coerceIn(0, 1)
            HudMode.AOE_NEW_SESSION_MENU -> newSessionIndex =
                (newSessionIndex + dir).coerceIn(0, newSessionOptions().lastIndex)
            HudMode.AOE_DICTATION_REVIEW -> dictation.moveFocus(dir)
            HudMode.AOE_DICTATION_LISTENING -> Unit
            else -> {
                scrollDir = dir
                scrollTick++
            }
        }
    }

    fun enterReplyMenu(s: Strings) {
        replyMenuIndex = 0
        mode = HudMode.AOE_REPLY_MENU
        status = s.reply
    }

    fun enterTextInput(s: Strings) {
        textInput = ""
        mode = HudMode.AOE_TEXT_INPUT
        status = s.textReply
    }

    fun appendText(ch: Char) { textInput += ch }
    fun backspaceText() { if (textInput.isNotEmpty()) textInput = textInput.dropLast(1) }

    fun enterNewSessionMenu(s: Strings) {
        newSessionIndex = 0
        creatingSession = false
        mode = HudMode.AOE_NEW_SESSION_MENU
        status = s.newSession
    }

    fun clear() {
        lines.clear()
        toolIndex.clear()
        lastWasText = false
    }
}

fun groupSessionsForDisplay(sessions: List<AoeSessionSummary>): List<AoeSessionSummary> =
    sessions.groupBy { it.group.ifBlank { "Scratch" } }.values.flatten()

@Composable
fun HudScreen(state: HudState, connStatus: String, s: Strings, connected: Boolean) {
    Box(Modifier.fillMaxSize().background(TerminalTokens.Background)) {
        val prompt = state.choice
        if (prompt != null) {
            PromptPage(state, prompt, s, connected)
        } else if (state.mode == HudMode.AOE_DICTATION_LISTENING) {
            val terminal = state.terminal
            DictationListeningPage(
                tool = terminal?.tool.orEmpty(),
                session = terminal?.title ?: s.sessionFallback,
                committed = state.dictation.committed,
                transcribing = state.dictation.phase == DictationPhase.TRANSCRIBING,
                s = s,
                connected = connected,
            )
        } else if (state.mode == HudMode.AOE_DICTATION_REVIEW) {
            val terminal = state.terminal
            DictationReviewPage(
                tool = terminal?.tool.orEmpty(),
                session = terminal?.title ?: s.sessionFallback,
                committed = state.dictation.committed,
                candidate = state.dictation.candidate,
                s = s,
                connected = connected,
                focusedAction = state.dictation.focusedAction,
            )
        } else {
            TerminalScaffold(
                header = headerTextFor(state, s),
                connected = connected,
                actionLabel = actionDockTextFor(state, s),
                actionFocused = actionDockFocusedFor(state.mode),
            ) {
                when (state.mode) {
                    HudMode.AOE_SESSIONS -> SessionsPage(state, s)
                    HudMode.AOE_TERMINAL -> TerminalPage(state)
                    HudMode.AOE_REPLY_MENU -> ReplyPage(state, s)
                    HudMode.AOE_TEXT_INPUT -> KeyboardPage(state)
                    HudMode.AOE_NEW_SESSION_MENU -> NewSessionPage(state)
                    HudMode.AOE_DICTATION_LISTENING, HudMode.AOE_DICTATION_REVIEW -> Unit
                }
            }
        }
        if (state.blanked) Box(Modifier.fillMaxSize().background(TerminalTokens.Background))
    }
}

fun localizedAoeStatus(raw: String, s: Strings): String = when (raw.uppercase()) {
    "WAITING", "RUN", "RUNNING", "ACTIVE" -> s.statusRunning
    "IDLE" -> s.statusIdle
    "STOPPED", "DONE", "COMPLETED" -> s.statusStopped
    "ERROR", "FAILED" -> s.statusError
    else -> raw.uppercase()
}

fun shortModel(model: String?, s: Strings): String = when {
    model == null -> s.modelUnknown
    model.contains("opus", true) -> "opus"
    model.contains("sonnet", true) -> "sonnet"
    model.contains("haiku", true) -> "haiku"
    else -> model
}

fun statuslineText(model: String?, costUsd: Double, tokens: Long, s: Strings): String {
    val cost = String.format("$%.2f", costUsd)
    val tokenText = if (tokens >= 1000) "${tokens / 1000}k tok" else "$tokens tok"
    return "${shortModel(model, s)} · ${s.sessionLabel} $cost · $tokenText"
}
