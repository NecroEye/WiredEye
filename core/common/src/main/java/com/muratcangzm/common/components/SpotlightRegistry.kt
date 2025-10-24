package com.muratcangzm.common.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

class SpotlightRegistry {
    private val _targets = mutableStateMapOf<String, Rect>()
    val targets: Map<String, Rect> get() = _targets

    fun update(id: String, rect: Rect) {
        _targets[id] = rect
    }

    fun get(id: String): Rect? = _targets[id]
}


class SpotlightController {
    var current by mutableStateOf<Rect?>(null)
        private set

    fun highlight(rect: Rect?) {
        current = rect
    }

    fun clear() {
        current = null
    }
}

internal fun SpotlightHitTest(
    registry: SpotlightRegistry,
    posWin: Offset
): Rect? = registry.targets.values
    .filter { it.contains(posWin) }
    .minByOrNull { it.width * it.height }