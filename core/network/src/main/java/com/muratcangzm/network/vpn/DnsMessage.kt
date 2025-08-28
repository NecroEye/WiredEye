package com.muratcangzm.network.vpn

import java.nio.ByteBuffer
import java.nio.ByteOrder

object DnsMessage {
    data class Question(val name: String, val type: String)
    data class Msg(val id: Int, val qr: Boolean, val questions: List<Question>)
    fun tryParse(bb: ByteBuffer): Msg? = runCatching { parse(bb) }.getOrNull()
    private fun parse(bb: ByteBuffer): Msg {
        bb.order(ByteOrder.BIG_ENDIAN)
        val id = bb.short.toInt() and 0xFFFF
        val flags = bb.short.toInt() and 0xFFFF
        val qr = (flags and 0x8000) != 0
        val qdCount = bb.short.toInt() and 0xFFFF
        bb.position(bb.position() + 6) // an/ns/ar
        val qs = mutableListOf<Question>()
        repeat(qdCount) {
            qs += Question(readName(bb), typeToString(bb.short.toInt() and 0xFFFF).also { bb.short })
        }
        return Msg(id, qr, qs)
    }
    private fun readName(bb: ByteBuffer): String {
        val parts = mutableListOf<String>()
        while (true) {
            val len = bb.get().toInt() and 0xFF
            if (len == 0) break
            val bytes = ByteArray(len).also { bb.get(it) }
            parts += String(bytes, Charsets.US_ASCII)
        }
        return parts.joinToString(".")
    }
    private fun typeToString(t: Int) = when (t) { 1 -> "A"; 28 -> "AAAA"; 5 -> "CNAME"; else -> "T$t" }
}