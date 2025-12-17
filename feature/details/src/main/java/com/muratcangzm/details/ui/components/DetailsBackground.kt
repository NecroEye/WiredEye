package com.muratcangzm.details.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import com.muratcangzm.resources.ui.theme.GhostColors

@Composable
internal fun DetailsBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = remember {
        Brush.verticalGradient(listOf(GhostColors.Bg0, GhostColors.Bg1, GhostColors.Bg0))
    }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .background(bg)
            .drawBehind {
                clipRect(
                    left = 0f,
                    right = size.width,
                    bottom = size.height
                ) {
                    val c1 = Offset(size.width * 1.02f, size.height * 0.12f)
                    val r1 = size.minDimension * 0.92f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GhostColors.Accent.copy(alpha = 0.06f),
                                GhostColors.Accent.copy(alpha = 0.00f)
                            ),
                            center = c1,
                            radius = r1
                        ),
                        center = c1,
                        radius = r1
                    )

                    val c2 = Offset(size.width * 0.10f, size.height * 0.80f)
                    val r2 = size.minDimension * 0.78f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GhostColors.AccentPink.copy(alpha = 0.045f),
                                GhostColors.AccentPink.copy(alpha = 0.00f)
                            ),
                            center = c2,
                            radius = r2
                        ),
                        center = c2,
                        radius = r2
                    )

                    val c3 = Offset(size.width * 0.86f, size.height * 0.92f)
                    val r3 = size.minDimension * 0.52f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GhostColors.AccentLime.copy(alpha = 0.03f),
                                GhostColors.AccentLime.copy(alpha = 0.00f)
                            ),
                            center = c3,
                            radius = r3
                        ),
                        center = c3,
                        radius = r3
                    )
                }
            }
    ) { content() }
}
