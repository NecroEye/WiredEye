package com.muratcangzm

import java.util.concurrent.atomic.AtomicLong

fun threadSafeOnClick(delay: Long = DEFAULT_DELAY, onClick: () -> Unit): () -> Unit {
    val latestClickTime = AtomicLong(0)
    return {
        val now = System.currentTimeMillis()
        if (now - latestClickTime.get() >= delay) {
            onClick()
            latestClickTime.set(now)
        }
    }
}

fun safeOnClick(delay: Long = DEFAULT_DELAY, onClick: () -> Unit): () -> Unit {
    var latestClickTime = 0L
    return {
        val now = System.currentTimeMillis()
        if (now - latestClickTime >= delay) {
            onClick()
            latestClickTime = now
        }
    }
}

const val DEFAULT_DELAY = 1500L