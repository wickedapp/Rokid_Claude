package com.rokid.relayhud

import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity

/** Locks the HUD to its approved 10sp typography regardless of system font scaling. */
@Composable
fun TerminalTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1f)) {
        ProvideTextStyle(TerminalTokens.Text, content)
    }
}
