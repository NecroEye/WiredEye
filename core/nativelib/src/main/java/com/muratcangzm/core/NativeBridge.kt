package com.muratcangzm.core

object NativeBridge {
    init { System.loadLibrary("wiredeye_native") }
    @JvmStatic external fun add(a: Int, b: Int): Int
}