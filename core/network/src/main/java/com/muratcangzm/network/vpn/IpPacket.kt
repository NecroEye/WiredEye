package com.muratcangzm.network.vpn

import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class IpPacket {
    data class Ipv4(val src: InetAddress, val dst: InetAddress, val protocol: Int, val payload: ByteBuffer) : IpPacket()
    companion object {
        fun parse(bb: ByteBuffer): IpPacket? {
            bb.order(ByteOrder.BIG_ENDIAN)
            val vhl = bb.get().toInt() and 0xFF
            val version = vhl ushr 4
            if (version != 4) return null
            val ihl = (vhl and 0x0F) * 4
            bb.position(bb.position() + 1) // TOS
            val totalLen = bb.short.toInt() and 0xFFFF
            bb.position(bb.position() + 5) // ID(2)+flags/frag(2)+TTL(1)
            val proto = bb.get().toInt() and 0xFF
            bb.position(bb.position() + 2) // checksum
            val src = ByteArray(4).also { bb.get(it) }
            val dst = ByteArray(4).also { bb.get(it) }
            bb.position(ihl)
            val payload = bb.slice().apply { limit(totalLen - ihl) }
            return Ipv4(InetAddress.getByAddress(src), InetAddress.getByAddress(dst), proto, payload)
        }
    }
}