package com.muratcangzm.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun StatusBarStyle(
    color: Color,
    darkIcons: Boolean
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(color, darkIcons) {
        val activity = view.context.findActivity()
        val window = activity?.window
        if (window != null) {
            window.statusBarColor = color.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        }
        onDispose { }
    }
}


@Composable
fun rememberSafeOnClick(
    delay: Long = DEFAULT_DELAY,
    onClick: () -> Unit
): () -> Unit {
    val lastClick = remember { mutableLongStateOf(0L) }
    val currentOnClick by rememberUpdatedState(onClick)

    return remember(delay) {
        {
            val now = SystemClock.elapsedRealtime()
            if (now - lastClick.longValue >= delay) {
                lastClick.longValue = now
                currentOnClick()
            }
        }
    }
}


private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

const val DEFAULT_DELAY = 1500L
