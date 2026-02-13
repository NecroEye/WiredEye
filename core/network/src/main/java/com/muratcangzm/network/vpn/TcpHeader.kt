package com.muratcangzm.network.vpn

import java.nio.ByteBuffer

internal data class TcpHeader(
    val srcPort: Int,
    val dstPort: Int,
    val headerLen: Int,
    val payload: ByteBuffer
) {
    companion object {
        fun parse(buffer: ByteBuffer): TcpHeader? {
            if (buffer.remaining() < 20) return null
            val start = buffer.position()
            val src = buffer.short.toInt() and 0xFFFF
            val dst = buffer.short.toInt() and 0xFFFF
            buffer.int
            buffer.int
            val dataOffset = (buffer.get().toInt() ushr 4) and 0x0F
            buffer.get()
            buffer.short
            buffer.short
            buffer.short
            val headerLength = dataOffset * 4
            if (headerLength < 20 || start + headerLength > buffer.limit()) {
                buffer.position(start)
                return null
            }
            val payloadSlice = buffer.duplicate().apply {
                position(start + headerLength)
                limit(buffer.limit())
            }.slice()
            buffer.position(start)
            return TcpHeader(src, dst, headerLength, payloadSlice)
        }
    }
}
