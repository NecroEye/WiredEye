package com.muratcangzm

import android.util.Log


inline fun <T> tryOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}

inline fun tryOrZero(block: () -> Double): Double {
    return try {
        block()
    } catch (e: Exception) {
        0.0
    }
}

inline fun <T> tryOrLog(block: () -> T) {
    try {
        block()
    } catch (e: Exception) {
        Log.d(e.cause?.message.orEmpty(), "tryOrLog: ${e.message} ")
    }
}