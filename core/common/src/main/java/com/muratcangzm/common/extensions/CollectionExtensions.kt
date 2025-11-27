package com.muratcangzm.common.extensions

fun LongArray?.orEmpty(): LongArray {
    return this ?: longArrayOf()
}

fun List<Any>.clear() {
    this.toMutableList().clear()
}