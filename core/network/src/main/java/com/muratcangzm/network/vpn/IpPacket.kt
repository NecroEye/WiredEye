// core/network/src/main/java/com/muratcangzm/network/vpn/IpPacket.kt
package com.muratcangzm.network.vpn

import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class IpPacket {
    data class Ipv4(
        val src: InetAddress,
        val dst: InetAddress,
        val protocol: Int,
        val payload: ByteBuffer
    ) : IpPacket()

    data class Ipv6(
        val src: InetAddress,
        val dst: InetAddress,
        val nextHeader: Int,
        val payload: ByteBuffer
    ) : IpPacket()

    companion object {
        fun parse(bb: ByteBuffer): IpPacket? {
            if (bb.remaining() < 1) return null
            val first = bb.get(bb.position()).toInt() and 0xFF
            return when ((first ushr 4) and 0x0F) {
                4 -> parseIpv4(bb)
                6 -> parseIpv6(bb)
                else -> null
            }
        }

        private fun parseIpv4(bb: ByteBuffer): Ipv4? {
            val pos0 = bb.position()
            bb.order(ByteOrder.BIG_ENDIAN)
            if (bb.remaining() < 20) return null

            val verIhl = bb.get().toInt() and 0xFF
            val ihl = (verIhl and 0x0F) * 4
            bb.get() // TOS
            val totalLen = bb.short.toInt() and 0xFFFF
            bb.int     // ID + flags/frag
            bb.get()   // TTL
            val proto = bb.get().toInt() and 0xFF
            bb.short   // checksum
            val src = ByteArray(4); bb.get(src)
            val dst = ByteArray(4); bb.get(dst)

            val payloadLen = (totalLen - ihl).coerceAtLeast(0)
            val end = pos0 + ihl + payloadLen
            if (end > bb.limit()) { bb.position(pos0); return null }

            val payload = bb.duplicate().apply {
                position(pos0 + ihl); limit(end)
            }.slice()
            bb.position(pos0)

            return Ipv4(
                InetAddress.getByAddress(src),
                InetAddress.getByAddress(dst),
                proto,
                payload
            )
        }

        private fun parseIpv6(bb: ByteBuffer): Ipv6? {
            val pos0 = bb.position()
            bb.order(ByteOrder.BIG_ENDIAN)
            if (bb.remaining() < 40) return null

            val vtcFlow = bb.int
            val ver = (vtcFlow ushr 28) and 0x0F
            if (ver != 6) { bb.position(pos0); return null }

            val payloadLen = bb.short.toInt() and 0xFFFF
            val next = bb.get().toInt() and 0xFF
            bb.get() // hop limit
            val src = ByteArray(16); bb.get(src)
            val dst = ByteArray(16); bb.get(dst)

            val startPayload = bb.position()
            val end = startPayload + payloadLen
            if (end > bb.limit()) { bb.position(pos0); return null }

            val payload = bb.duplicate().apply {
                position(startPayload); limit(end)
            }.slice()
            bb.position(pos0)

            return Ipv6(
                InetAddress.getByAddress(src),
                InetAddress.getByAddress(dst),
                next,
                payload
            )
        }
    }
}