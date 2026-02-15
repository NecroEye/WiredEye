package com.muratcangzm.leaks.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object LeaksTokens {
    val TopBar = Color(0xFF071018)
    val BackA = Color(0xFF050B12)
    val BackB = Color(0xFF071018)
    val Surface = Color(0x14101826)
    val SurfaceStrong = Color(0x1F101826)
    val Border = Color(0xFF233355).copy(alpha = 0.55f)
    val TextStrong = Color(0xFFDBEAFE)
    val TextDim = Color(0xFF9EB2C0)
    val Accent = Color(0xFF7BD7FF)
    val Good = Color(0xFF30E3A2)
    val Warn = Color(0xFFFFC107)
    val Danger = Color(0xFFFF6B6B)

    val Corner = 18.dp
    val CornerSmall = 14.dp

    val BorderBrush = Brush.linearGradient(
        listOf(
            Color(0xFF233355).copy(alpha = 0.68f),
            Accent.copy(alpha = 0.34f),
            Color(0xFF233355).copy(alpha = 0.68f)
        )
    )
}
