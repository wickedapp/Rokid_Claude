package com.rokid.relayhud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

private data class SessionDisplayRow(
    val kind: String,
    val sessionIndex: Int = Int.MIN_VALUE,
    val text: String,
)

private fun fitSessionCell(value: String, width: Int): String {
    val clean = value.replace('\n', ' ').replace('\t', ' ').trim()
    return when {
        clean.length == width -> clean
        clean.length < width -> clean.padEnd(width)
        else -> clean.take((width - 1).coerceAtLeast(0)) + "…"
    }
}

private fun compactStatus(status: String): String = when (status.lowercase()) {
    "running" -> "RUN"
    "waiting" -> "WAIT"
    "idle", "stopped" -> "IDLE"
    "error", "failed" -> "ERR"
    else -> status.uppercase().take(4).ifBlank { "-" }
}

internal fun compactSessionRows(sessions: List<AoeSessionSummary>, s: Strings): List<String> = buildList {
    add(" TOOL     GROUP     SESSION             ST   AGE")
    add("[+] ${s.newSession}")
    sessions.forEach { item ->
        val mark = if (item.unread) "*" else " "
        val tool = fitSessionCell(displayToolName(item.tool), 8)
        val group = fitSessionCell(item.group.ifBlank { s.scratchGroup }, 9)
        val title = fitSessionCell(item.title, 19)
        val status = fitSessionCell(compactStatus(item.status), 4)
        val age = fitSessionCell(item.age, 4)
        add("$mark$tool $group $title $status $age")
    }
}

private fun sessionDisplayRows(sessions: List<AoeSessionSummary>, s: Strings): List<SessionDisplayRow> {
    val lines = compactSessionRows(sessions, s)
    return buildList {
        add(SessionDisplayRow("header", text = lines[0]))
        add(SessionDisplayRow("action", -1, lines[1]))
        sessions.indices.forEach { index ->
            add(SessionDisplayRow("session", index, lines[index + 2]))
        }
    }
}

@Composable
fun SessionsPage(state: HudState, s: Strings) {
    val listState = rememberLazyListState()
    val rows = remember(state.aoeSessions.toList(), s) { sessionDisplayRows(state.aoeSessions, s) }
    val focusedRow = rows.indexOfFirst { it.sessionIndex == state.selectedSessionIndex }.coerceAtLeast(0)
    LaunchedEffect(focusedRow, rows.size) {
        if (rows.isNotEmpty()) listState.animateScrollToItem(focusedRow)
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
        items(rows) { row ->
            val focused = row.sessionIndex == state.selectedSessionIndex
            if (row.kind == "header") {
                Box(
                    Modifier.fillMaxWidth().height(TerminalTokens.ActionHeight).terminalBottomDivider()
                        .padding(horizontal = TerminalTokens.Space4),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(row.text, style = TerminalTokens.MutedText, maxLines = 1, overflow = TextOverflow.Clip)
                }
            } else {
                TerminalMenuRow(row.text, focused)
            }
        }
    }
}
