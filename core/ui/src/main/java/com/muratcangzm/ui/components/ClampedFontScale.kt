package com.muratcangzm.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
fun ClampedFontScale(
    minScale: Float = 1.0f,
    maxScale: Float = 1.20f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val clamped = density.fontScale.coerceIn(minScale, maxScale)

    CompositionLocalProvider(
        LocalDensity provides Density(density.density, clamped)
    ) {
        content()
    }
}
