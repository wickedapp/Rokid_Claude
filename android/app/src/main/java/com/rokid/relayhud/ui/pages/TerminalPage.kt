package com.rokid.relayhud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

private const val TERMINAL_VISIBLE_LINES = 32
private const val TERMINAL_DISPLAY_WIDTH = 70

private fun terminalCellWidth(char: Char): Int = when (char.code) {
    in 0x1100..0x115F,
    in 0x2329..0x232A,
    in 0x2E80..0xA4CF,
    in 0xAC00..0xD7A3,
    in 0xF900..0xFAFF,
    in 0xFE10..0xFE19,
    in 0xFE30..0xFE6F,
    in 0xFF00..0xFF60,
    in 0xFFE0..0xFFE6 -> 2
    else -> 1
}

private fun terminalDisplayWidth(text: String): Int = text.sumOf(::terminalCellWidth)

private fun charsThatFit(text: String, width: Int): Int {
    var cells = 0
    text.forEachIndexed { index, char ->
        val next = cells + terminalCellWidth(char)
        if (next > width) return index
        cells = next
    }
    return text.length
}

fun terminalRows(content: String): List<String> = content.split('\n')
    .flatMap { wrapTerminalLine(it.replace('\t', ' ').replace('\u001B'.toString(), "").trimEnd(), TERMINAL_DISPLAY_WIDTH) }
    .dropLastWhile { it.isBlank() }

fun wrapTerminalLine(line: String, width: Int = TERMINAL_DISPLAY_WIDTH): List<String> {
    val clean = line.replace('\t', ' ').replace('\u001B'.toString(), "")
    if (clean.isEmpty()) return listOf(" ")
    val out = mutableListOf<String>()
    var rest = clean
    while (terminalDisplayWidth(rest) > width) {
        val fitChars = charsThatFit(rest, width).coerceAtLeast(1)
        val candidate = rest.take(fitChars)
        val whitespace = candidate.indexOfLast { it.isWhitespace() }
        val breakAt = if (whitespace > 0) whitespace else fitChars
        out += rest.take(breakAt).trimEnd()
        rest = rest.drop(if (whitespace > 0) breakAt + 1 else breakAt).trimStart()
    }
    out += rest
    return out
}

fun terminalBottomStart(content: String, visibleCount: Int = TERMINAL_VISIBLE_LINES): Int =
    (terminalRows(content).size - visibleCount).coerceAtLeast(0)

@Composable
fun TerminalPage(state: HudState) {
    val rows = remember(state.terminal?.content) { terminalRows(state.terminal?.content.orEmpty()) }
    val maxStart = (rows.size - TERMINAL_VISIBLE_LINES).coerceAtLeast(0)
    val start = state.terminalScroll.coerceIn(0, maxStart)
    val window = rows.drop(start).take(TERMINAL_VISIBLE_LINES)
    val lines = if (window.size < TERMINAL_VISIBLE_LINES) {
        List(TERMINAL_VISIBLE_LINES - window.size) { " " } + window
    } else {
        window
    }
    Column(Modifier.fillMaxSize()) {
        lines.forEach { line ->
            Text(
                line.ifEmpty { " " },
                style = TerminalTokens.Text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
