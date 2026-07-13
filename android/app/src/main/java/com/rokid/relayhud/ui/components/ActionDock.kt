package com.rokid.relayhud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.drawBehind

@Composable
fun ActionDock(label: String, focused: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(TerminalTokens.FooterHeight)
            .background(TerminalTokens.Background).terminalBottomDivider(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.wrapContentWidth().height(TerminalTokens.ActionHeight)
                .background(if (focused) TerminalTokens.FocusBackground else TerminalTokens.Background)
                .padding(horizontal = TerminalTokens.Space4),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = if (focused) TerminalTokens.FocusText else TerminalTokens.Text, maxLines = 1)
        }
    }
}

@Composable
fun ReviewActionDock(
    actions: List<String>,
    focusedIndex: Int = DEFAULT_DICTATION_REVIEW_FOCUS,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().height(TerminalTokens.FooterGridHeight)) {
        actions.chunked(2).forEachIndexed { rowIndex, rowActions ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                rowActions.forEachIndexed { columnIndex, action ->
                    val index = rowIndex * 2 + columnIndex
                    val focused = index == focusedIndex
                    Box(
                        Modifier.weight(1f).height(TerminalTokens.FooterGridHeight / 2)
                            .background(if (focused) TerminalTokens.FocusBackground else TerminalTokens.Background)
                            .border(TerminalTokens.DividerWidth, TerminalTokens.Divider, RectangleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(action, style = if (focused) TerminalTokens.FocusText else TerminalTokens.Text, maxLines = 1)
                    }
                }
            }
        }
    }
}

internal fun Modifier.terminalBottomDivider(): Modifier = drawBehind {
    val stroke = TerminalTokens.DividerWidth.toPx()
    drawLine(
        color = TerminalTokens.Divider,
        start = androidx.compose.ui.geometry.Offset(0f, size.height - stroke / 2f),
        end = androidx.compose.ui.geometry.Offset(size.width, size.height - stroke / 2f),
        strokeWidth = stroke,
    )
}
