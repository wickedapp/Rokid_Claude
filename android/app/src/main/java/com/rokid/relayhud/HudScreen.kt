package com.rokid.relayhud

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Green = Color(0xFF00FF00)
val DimGreen = Color(0xFF7CE07C)
val FocusBg = Color(0x3328FF48)
private const val TERMINAL_VISIBLE_LINES = 36

data class HudLine(val text: String, val color: Color = Green)
enum class HudMode { AOE_SESSIONS, AOE_TERMINAL, AOE_REPLY_MENU, AOE_TEXT_INPUT, AOE_NEW_SESSION_MENU }

/** 权限选择模式的当前状态(高亮项与剩余秒数由 MainActivity 更新)。 */
data class PermissionPrompt(
    val id: String,
    val summary: String,
    val options: List<String>,
    val allowKey: String,
    val highlight: Int,
    val secondsLeft: Int,
    val title: String = "需要确认",
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
    var textInput by mutableStateOf("")
    var terminalScroll by mutableStateOf(0)
        private set
    var status by mutableStateOf("")
    val toolIndex = mutableMapOf<String, Int>()
    var recording by mutableStateOf(false)
    var choice by mutableStateOf<PermissionPrompt?>(null)
    var blanked by mutableStateOf(false)
    var statusline by mutableStateOf("")
    var scrollTick by mutableStateOf(0)
        private set
    var scrollDir = 0
        private set

    private var lastWasText = false

    fun add(text: String, color: Color = Green) { lines.add(HudLine(text, color)); lastWasText = false }
    fun addText(text: String) {
        val parts = text.split("\n")
        parts.forEachIndexed { i, part ->
            if (i == 0 && lastWasText && lines.isNotEmpty()) {
                val last = lines.size - 1
                lines[last] = lines[last].copy(text = lines[last].text + part)
            } else lines.add(HudLine(part, Green))
        }
        lastWasText = true
    }

    fun setAoeSessions(items: List<AoeSessionSummary>) {
        // The visible AoE sidebar is grouped, so selection indices must follow that same
        // grouped order. Keeping the original recency indices made DPAD jump from the
        // first Valetax rows to Halley and left later Valetax rows unreachable.
        val ordered = groupSessionsForDisplay(items)
        aoeSessions.clear(); aoeSessions.addAll(ordered)
        if (selectedSessionIndex >= aoeSessions.size) selectedSessionIndex = (aoeSessions.size - 1).coerceAtLeast(0)
        if (mode != HudMode.AOE_TERMINAL && mode != HudMode.AOE_REPLY_MENU && mode != HudMode.AOE_TEXT_INPUT) mode = HudMode.AOE_SESSIONS
        val clde = ordered.count { it.tool.contains("claude", true) }
        val cdex = ordered.count { it.tool.contains("codex", true) }
        status = "aoe - Recent   CLDE $clde  CDEX $cdex"
    }

    fun moveSession(delta: Int) {
        val min = -2 // -2 = + Claude, -1 = + Codex
        val max = (aoeSessions.size - 1).coerceAtLeast(min)
        selectedSessionIndex = (selectedSessionIndex + delta).coerceIn(min, max)
    }

    fun showTerminal(snapshot: AoeTerminalSnapshot) {
        val previous = terminal
        val sameSession = previous?.id == snapshot.id
        val shouldFollowBottom = !sameSession || mode != HudMode.AOE_TERMINAL || terminalAtBottom()
        terminal = snapshot
        activeSessionId = snapshot.id
        terminalScroll = if (shouldFollowBottom) {
            terminalBottomStart(snapshot.content)
        } else {
            terminalScroll.coerceIn(0, terminalBottomStart(snapshot.content))
        }
        mode = HudMode.AOE_TERMINAL
        status = "${snapshot.tool.uppercase()} / ${snapshot.title} / ${snapshot.status.uppercase()}"
    }

    fun terminalLooksInteractive(): Boolean {
        val text = terminal?.content?.takeLast(1500)?.lowercase() ?: return false
        return listOf(
            "❯", "›", "use arrow", "arrow keys", "press enter", "enter to",
            "do you want", "would you like", "choose an option", "select an option",
            "❯ 1", "❯ yes", "❯ no"
        ).any { text.contains(it) }
    }

    fun terminalAtBottom(): Boolean {
        val content = terminal?.content ?: return true
        val maxStart = terminalBottomStart(content)
        return terminalScroll >= maxStart - 1
    }

    fun scroll(dir: Int) {
        when (mode) {
            HudMode.AOE_TERMINAL -> terminalScroll = (terminalScroll + dir).coerceIn(0, 10000)
            HudMode.AOE_REPLY_MENU -> replyMenuIndex = (replyMenuIndex + dir).coerceIn(0, 2)
            HudMode.AOE_NEW_SESSION_MENU -> newSessionIndex = (newSessionIndex + dir).coerceIn(0, 2)
            else -> { scrollDir = dir; scrollTick++ }
        }
    }

    fun enterReplyMenu() { replyMenuIndex = 0; mode = HudMode.AOE_REPLY_MENU; status = "Reply" }
    fun enterTextInput() { textInput = ""; mode = HudMode.AOE_TEXT_INPUT; status = "Text reply" }
    fun appendText(ch: Char) { textInput += ch }
    fun backspaceText() { if (textInput.isNotEmpty()) textInput = textInput.dropLast(1) }
    fun enterNewSessionMenu() { newSessionIndex = 0; mode = HudMode.AOE_NEW_SESSION_MENU; status = "New session" }

    fun clear() { lines.clear(); toolIndex.clear(); lastWasText = false }
}

@Composable
fun HudScreen(state: HudState, connStatus: String, s: Strings, connected: Boolean) {
    val body = TextStyle(color = Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    val meta = TextStyle(color = DimGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 30.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("AOE TERM", style = meta.copy(color = Green), maxLines = 1, softWrap = false)
                Text(connStatus.uppercase(), style = meta, maxLines = 1, softWrap = false)
            }
            Text(state.status, style = meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                when (state.mode) {
                    HudMode.AOE_TERMINAL -> AoeTerminalView(state, body, meta)
                    HudMode.AOE_REPLY_MENU -> AoeReplyMenu(state, body, meta)
                    HudMode.AOE_TEXT_INPUT -> AoeTextInput(state, body, meta)
                    HudMode.AOE_NEW_SESSION_MENU -> AoeNewSessionMenu(state, body, meta)
                    else -> AoeSessionList(state, body, meta)
                }
            }
            val footer = when (state.mode) {
                HudMode.AOE_TERMINAL -> if (state.terminalLooksInteractive()) "↑↓ SELECT  ENTER SEND  TYPE INPUT  BACK SESSIONS" else "↑↓ SCROLL  ENTER REPLY  BACK SESSIONS"
                HudMode.AOE_REPLY_MENU, HudMode.AOE_NEW_SESSION_MENU -> "↑↓ CHOOSE  ENTER OK  BACK CANCEL"
                HudMode.AOE_TEXT_INPUT -> "TYPE TEXT  ENTER SEND  BACK CANCEL"
                else -> "↑↓ MOVE  ENTER OPEN/NEW  BACK EXIT"
            }
            Text(
                if (connected) footer else s.offlineHint,
                style = meta.copy(color = Green, fontSize = 9.sp),
                maxLines = 1,
                softWrap = false,
            )
        }

        state.choice?.let { p ->
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.fillMaxWidth(0.86f).border(1.dp, Green, RoundedCornerShape(14.dp)).padding(14.dp),
                ) {
                    Text("${p.title} (${p.secondsLeft}s)", style = meta)
                    Text(p.summary, style = body.copy(fontSize = 14.sp), modifier = Modifier.padding(vertical = 6.dp))
                    p.options.forEachIndexed { i, opt ->
                        val sel = i == p.highlight
                        Text((if (sel) "▸ " else "   ") + opt, style = body.copy(color = if (sel) Green else DimGreen))
                    }
                    Text(s.choiceHint, style = meta, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        if (state.blanked) Box(Modifier.fillMaxSize().background(Color.Black))
    }
}

private fun fitCell(value: String, width: Int): String {
    val clean = value.replace('\n', ' ').replace('\t', ' ').trim()
    if (clean.length == width) return clean
    if (clean.length < width) return clean.padEnd(width)
    return clean.take((width - 1).coerceAtLeast(0)) + "…"
}

private fun toolCode(tool: String): String = when {
    tool.contains("codex", true) -> "CDEX"
    tool.contains("claude", true) -> "CLDE"
    tool.contains("open", true) -> "OPEN"
    else -> tool.uppercase().take(4).padEnd(4)
}

private data class SessionDisplayRow(
    val kind: String,
    val sessionIndex: Int = -99,
    val text: String,
)

private fun sessionRows(sessions: List<AoeSessionSummary>): List<SessionDisplayRow> {
    val rows = mutableListOf<SessionDisplayRow>()
    rows += SessionDisplayRow("action", -2, "+ New Claude session")
    rows += SessionDisplayRow("action", -1, "+ New Codex session")
    sessions.groupBy { it.group.ifBlank { "Scratch" } }.forEach { (group, items) ->
        rows += SessionDisplayRow("header", -99, "▾ $group (${items.size})")
        items.forEach { item ->
            val idx = sessions.indexOf(item)
            val mark = if (item.unread) "*" else " "
            val status = item.status.uppercase().let { if (it == "WAITING") "RUN" else it }
            rows += SessionDisplayRow(
                "session",
                idx,
                "$mark ${toolCode(item.tool)} ${fitCell(item.title, 27)} ${fitCell(item.age.ifBlank { status }, 4)}"
            )
        }
    }
    return rows
}

internal fun groupSessionsForDisplay(sessions: List<AoeSessionSummary>): List<AoeSessionSummary> =
    sessions.groupBy { it.group.ifBlank { "Scratch" } }.values.flatten()

internal fun terminalRows(content: String): List<String> =
    content.split('\n').map { it.replace('\t', ' ').replace('\u001B'.toString(), "").trimEnd() }

internal fun wrapTerminalLine(line: String, width: Int = 64): List<String> {
    // Unit-test helper / fallback only. The live Terminal View renders AoE terminal
    // rows directly and does not reflow them in Android.
    val clean = line.replace('\t', ' ').replace('\u001B'.toString(), "")
    if (clean.isEmpty()) return listOf(" ")
    val out = mutableListOf<String>()
    var rest = clean
    while (rest.length > width) {
        val window = rest.take(width + 1)
        val breakAt = window.dropLast(1).indexOfLast { it.isWhitespace() }
        if (breakAt > 0) {
            out += rest.take(breakAt).trimEnd()
            rest = rest.drop(breakAt + 1).trimStart()
        } else {
            out += rest.take(width)
            rest = rest.drop(width).trimStart()
        }
    }
    out += rest
    return out
}

internal fun terminalBottomStart(content: String, visibleCount: Int = TERMINAL_VISIBLE_LINES): Int {
    val count = terminalRows(content).size
    return (count - visibleCount).coerceAtLeast(0)
}

@Composable
private fun AoeSessionList(state: HudState, body: TextStyle, meta: TextStyle) {
    val listState = rememberLazyListState()
    val rows = remember(state.aoeSessions.toList(), state.selectedSessionIndex) { sessionRows(state.aoeSessions) }
    val focusedRow = rows.indexOfFirst { it.sessionIndex == state.selectedSessionIndex }.coerceAtLeast(0)
    LaunchedEffect(focusedRow, rows.size) { if (rows.isNotEmpty()) listState.animateScrollToItem(focusedRow) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(rows) { _, row ->
            val focus = row.sessionIndex == state.selectedSessionIndex
            val style = when (row.kind) {
                "header" -> meta.copy(color = DimGreen, fontSize = 10.sp)
                "action" -> body.copy(color = if (focus) Color.Black else Green, fontSize = 11.sp)
                else -> body.copy(color = if (focus) Color.Black else Green, fontSize = 11.sp)
            }
            Text(
                text = (if (focus) ">" else " ") + row.text,
                style = style,
                modifier = Modifier.fillMaxWidth()
                    .background(if (focus) Green else Color.Transparent)
                    .padding(horizontal = 1.dp, vertical = if (row.kind == "header") 2.dp else 1.dp),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun AoeTerminalView(state: HudState, body: TextStyle, meta: TextStyle) {
    val terminal = state.terminal
    val listState = rememberLazyListState()
    val rows = remember(terminal?.content) { terminal?.content?.let { terminalRows(it) } ?: emptyList() }
    val visibleCount = TERMINAL_VISIBLE_LINES
    val maxStart = (rows.size - visibleCount).coerceAtLeast(0)
    val start = state.terminalScroll.coerceIn(0, maxStart)
    val lines = rows.drop(start).take(visibleCount)
    LaunchedEffect(terminal?.id, terminal?.content, state.terminalScroll) { listState.scrollToItem(0) }
    Text(
        "${toolCode(terminal?.tool ?: "AOE")} ${fitCell(terminal?.title ?: "", 21)} ${fitCell((terminal?.status ?: "").uppercase(), 6)} ${start + 1}/${rows.size.coerceAtLeast(1)}",
        style = meta.copy(color = Green),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(Color.Black)) {
        itemsIndexed(lines) { _, line ->
            Text(
                line.ifEmpty { " " },
                style = body.copy(fontSize = 6.sp),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun AoeReplyMenu(state: HudState, body: TextStyle, meta: TextStyle) {
    val opts = listOf("Voice dictation", "Text keyboard", "Cancel")
    Column(Modifier.fillMaxSize().padding(top = 20.dp), horizontalAlignment = Alignment.Start) {
        Text("REPLY TO ${state.terminal?.title ?: "SESSION"}", style = meta.copy(color = Green))
        Spacer(Modifier.height(8.dp))
        opts.forEachIndexed { i, opt ->
            val focus = i == state.replyMenuIndex
            Text(
                (if (focus) ">" else " ") + opt,
                style = body.copy(color = if (focus) Color.Black else Green, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth().background(if (focus) Green else Color.Transparent).padding(2.dp),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("↑↓ choose · ENTER confirm · BACK terminal", style = meta)
    }
}

@Composable
private fun AoeTextInput(state: HudState, body: TextStyle, meta: TextStyle) {
    Column(Modifier.fillMaxSize().padding(top = 10.dp)) {
        Text("TEXT REPLY", style = meta.copy(color = Green))
        Spacer(Modifier.height(8.dp))
        Text("> ${state.textInput.ifBlank { "_" }}", style = body.copy(fontSize = 12.sp), modifier = Modifier.fillMaxWidth().background(Color.Black))
        Spacer(Modifier.height(8.dp))
        Text("BT keyboard: type · ENTER send · BACK cancel", style = meta)
    }
}

@Composable
private fun AoeNewSessionMenu(state: HudState, body: TextStyle, meta: TextStyle) {
    val opts = listOf("Claude", "Codex", "Cancel")
    Column(Modifier.fillMaxSize().padding(top = 20.dp)) {
        Text("NEW SESSION", style = meta.copy(color = Green))
        Spacer(Modifier.height(8.dp))
        opts.forEachIndexed { i, opt ->
            val focus = i == state.newSessionIndex
            Text(
                (if (focus) ">" else " ") + opt,
                style = body.copy(color = if (focus) Color.Black else Green, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth().background(if (focus) Green else Color.Transparent).padding(2.dp),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("creates scratch session; web API/aoe add compatible", style = meta)
    }
}

/** 模型 id 或别名 → 短名;组装 statusline 文本。 */
fun shortModel(model: String?, s: Strings): String = when {
    model == null -> s.modelUnknown
    model.contains("opus", true) -> "opus"
    model.contains("sonnet", true) -> "sonnet"
    model.contains("haiku", true) -> "haiku"
    else -> model
}
fun statuslineText(model: String?, costUsd: Double, tokens: Long, s: Strings): String {
    val cost = String.format("$%.2f", costUsd)
    val tok = if (tokens >= 1000) "${tokens / 1000}k tok" else "$tokens tok"
    return "${shortModel(model, s)} · ${s.sessionLabel} $cost · $tok"
}
