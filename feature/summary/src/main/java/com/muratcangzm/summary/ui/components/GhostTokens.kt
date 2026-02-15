// feature/summary/src/main/java/com/muratcangzm/summary/ui/components/GhostTokens.kt
package com.muratcangzm.summary.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Immutable
object SummaryTokens {
    val Surface = Color(0x22101826)
    val SurfaceStrong = Color(0x2A101826)
    val SurfaceWeak = Color(0x14101826)

    val Border = Color(0xFF233355)
    val BorderSoft = Color(0xFF233355).copy(alpha = 0.55f)

    val TextDim = Color(0xFF9EB2C0)
    val TextBright = Color(0xFFDBEAFE)
    val TextMid = Color(0xFFC9D6E2)

    val Accent = Color(0xFF7BD7FF)
    val Accent2 = Color(0xFFFFA6E7)
    val Accent3 = Color(0xFFBCE784)

    val BgTop = Color(0xFF0E141B)
    val BgMid = Color(0xFF0B1022)
    val BgBottom = Color(0xFF070B14)

    val ShapeChip = RoundedCornerShape(18.dp)
    val ShapeCard = RoundedCornerShape(20.dp)
    val ShapePill = RoundedCornerShape(16.dp)
}
