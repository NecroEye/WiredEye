package com.muratcangzm.network.vpn

import java.nio.ByteBuffer

object UdpHeader {
    data class Header(val srcPort: Int, val dstPort: Int, val length: Int, val payload: ByteBuffer)
    fun parse(bb: ByteBuffer): Header? {
        if (bb.remaining() < 8) return null
        val src = bb.short.toInt() and 0xFFFF
        val dst = bb.short.toInt() and 0xFFFF
        val len = bb.short.toInt() and 0xFFFF
        bb.short // checksum
        val payload = bb.slice().apply { limit((len - 8).coerceAtLeast(0)) }
        return Header(src, dst, len, payload)
    }
}