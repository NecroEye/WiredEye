package com.muratcangzm.common.extensions

fun <T> List<T>.parseListToString() = this.joinToString(separator = ", ", prefix = "[", postfix = "]")

public fun <T> List<T>.second(): T {
    if (isEmpty())
        throw NoSuchElementException("List is empty.")
    return this[1]
}

fun <T> List<T>.chunkedFixedSize(size: Int): List<List<T?>> {
    return this.chunked(size).map { chunk ->
        if (chunk.size < size) chunk + List(size - chunk.size) { null } else chunk
    }
}