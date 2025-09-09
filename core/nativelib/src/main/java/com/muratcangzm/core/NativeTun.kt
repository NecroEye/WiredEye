package com.muratcangzm.core

import androidx.annotation.Keep

@Keep
object NativeTun {

    @Keep
    interface Listener {
        fun onNativeBatch(buf: ByteArray, validBytes: Int, packetCount: Int)
    }

    @JvmStatic external fun nativeSetListener(listener: Listener?)
    @JvmStatic external fun nativeStart(
        detachedTunFd: Int,
        mtu: Int,
        maxBatch: Int,
        maxBatchBytes: Int,
        flushTimeoutMs: Int,
        readTimeoutMs: Int
    ): Boolean
    @JvmStatic external fun nativeStop()

    fun setListener(l: Listener?) = nativeSetListener(l)

    fun start(
        detachedTunFd: Int,
        mtu: Int,
        maxBatch: Int,
        maxBatchBytes: Int,
        flushTimeoutMs: Int,
        readTimeoutMs: Int
    ): Boolean = nativeStart(detachedTunFd, mtu, maxBatch, maxBatchBytes, flushTimeoutMs, readTimeoutMs)

    fun stop() = nativeStop()

    init {
        System.loadLibrary("wiredeye_native")
    }
}