package com.muratcangzm.wiredeye.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.muratcangzm.ui.components.GhostColorTokens

private fun ghostColorScheme() = darkColorScheme(
    primary = GhostColorTokens.Accent,
    onPrimary = GhostColorTokens.TextPrimary,

    secondary = GhostColorTokens.AccentSoft,
    onSecondary = GhostColorTokens.TextPrimary,

    background = GhostColorTokens.Background,
    onBackground = GhostColorTokens.TextPrimary,

    surface = GhostColorTokens.Surface,
    onSurface = GhostColorTokens.TextPrimary,

    surfaceVariant = GhostColorTokens.SurfaceSoft,
    outline = GhostColorTokens.Border,

    error = GhostColorTokens.Error
)


@Composable
fun WiredEyeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ghostColorScheme(),
        typography = Typography,
        content = content
    )
}
