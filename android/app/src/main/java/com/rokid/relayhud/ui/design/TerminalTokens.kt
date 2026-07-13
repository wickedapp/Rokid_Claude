package com.rokid.relayhud

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Approved 480x640 Rokid AOE terminal design tokens. */
object TerminalTokens {
    val Background = Color.Black
    val Foreground = Color(0xFF00FF40)
    val Muted = Color(0xFF78DA8C)
    val Divider = Color(0xFF164C22)
    val FocusBackground = Foreground
    val FocusForeground = Color.Black

    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val SafeTop = 36.dp
    val SafeBottom = 24.dp
    val PageInset = 4.dp
    val HeaderHeight = 28.dp
    val FooterHeight = 24.dp
    val FooterGridHeight = 56.dp
    val ActionHeight = 20.dp
    val MenuRowHeight = 24.dp
    val DividerWidth = 1.dp

    val Text = TextStyle(
        color = Foreground,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )
    val FocusText = Text.copy(color = FocusForeground, fontWeight = FontWeight.Medium)
    val MutedText = Text.copy(color = Muted)
}
