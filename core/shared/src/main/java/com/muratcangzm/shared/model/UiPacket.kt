package com.muratcangzm.shared.model

import androidx.compose.runtime.Immutable
import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.serialization.Serializable


@Immutable
@Serializable
data class UiPacket(
    val key: String,
    val time: String,
    val app: String,
    val proto: String,
    val from: String,
    val to: String,
    val bytesLabel: String,
    val raw: PacketMeta
){

    companion object {
        val EMPTY: UiPacket = UiPacket(
            key = "",
            time = "",
            app = "",
            proto = "",
            from = "",
            to = "",
            bytesLabel = "",
           raw = PacketMeta()
        )
        fun empty(): UiPacket = EMPTY
    }
}