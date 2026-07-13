package com.rokid.relayhud

/** Public, platform-free presentation helpers used by both Compose and unit tests. */
fun displayToolName(tool: String): String = when {
    tool.contains("codex", ignoreCase = true) -> "CODEX"
    tool.contains("claude", ignoreCase = true) -> "CLAUDE"
    tool.contains("opencode", ignoreCase = true) || tool.contains("open code", ignoreCase = true) -> "OPENCODE"
    else -> tool.trim().uppercase().ifBlank { "AOE" }
}

fun replyMethodOptions(s: Strings): List<String> = listOf(s.voiceDictation, s.textKeyboard)

fun newSessionOptions(): List<String> = listOf("CLAUDE", "CODEX", "OPENCODE")

const val DEFAULT_DICTATION_REVIEW_FOCUS = 1

fun dictationReviewActions(s: Strings): List<String> =
    listOf(s.send, s.continueAction, s.redo, s.cancelSegment)

data class TerminalRegionBounds(
    val header: IntRange,
    val body: IntRange,
    val footer: IntRange,
)

/** Deterministic 480x640 scaffold contract used by unit tests and screenshot acceptance. */
fun terminalRegionBounds(totalHeight: Int = 640, reviewFooter: Boolean = false): TerminalRegionBounds {
    val safeTop = 36
    val safeBottom = 24
    val headerHeight = 28
    val footerHeight = if (reviewFooter) 56 else 24
    val gap = 4
    val header = safeTop until safeTop + headerHeight
    val bodyStart = header.last + 1 + gap
    val footerStart = totalHeight - safeBottom - footerHeight
    val body = bodyStart until footerStart - gap
    val footer = footerStart until totalHeight - safeBottom
    return TerminalRegionBounds(header, body, footer)
}

fun headerTextFor(state: HudState, s: Strings): String {
    if (state.mode == HudMode.AOE_NEW_SESSION_MENU) {
        val tool = newSessionOptions()[state.newSessionIndex.coerceIn(0, newSessionOptions().lastIndex)]
        return "$tool / ${s.newSession}"
    }
    val terminal = state.terminal
    if (state.mode == HudMode.AOE_SESSIONS) return "${s.recent} / ${state.aoeSessions.size}"
    val tool = displayToolName(terminal?.tool.orEmpty())
    val title = terminal?.title?.ifBlank { s.sessionFallback } ?: s.sessionFallback
    val pageStatus = when (state.mode) {
        HudMode.AOE_TERMINAL -> localizedAoeStatus(terminal?.status.orEmpty(), s)
        HudMode.AOE_REPLY_MENU -> s.reply
        HudMode.AOE_TEXT_INPUT -> s.textKeyboard
        HudMode.AOE_NEW_SESSION_MENU -> s.newSession
        HudMode.AOE_DICTATION_LISTENING -> s.listening
        HudMode.AOE_DICTATION_REVIEW -> s.dictationReview
        HudMode.AOE_SESSIONS -> error("handled above")
    }
    return "$tool / $title / $pageStatus"
}

fun actionDockTextFor(state: HudState, s: Strings): String {
    if (state.mode == HudMode.AOE_TERMINAL) {
        return if (state.terminalLooksInteractive()) {
            "↑↓ ${s.select}  [ENTER] ${s.confirmAction}  [BACK] ${s.sessions}"
        } else {
            "↑↓ ${s.scroll}  [ENTER] ${s.reply}  [BACK] ${s.sessions}"
        }
    }
    val action = when (state.mode) {
        HudMode.AOE_SESSIONS -> if (state.selectedSessionIndex < 0) s.create else s.open
        HudMode.AOE_REPLY_MENU, HudMode.AOE_NEW_SESSION_MENU -> s.confirmAction
        HudMode.AOE_TEXT_INPUT -> s.send
        HudMode.AOE_DICTATION_LISTENING -> s.stopSegment
        HudMode.AOE_DICTATION_REVIEW -> s.confirmAction
        HudMode.AOE_TERMINAL -> error("handled above")
    }
    return "[ENTER] $action"
}