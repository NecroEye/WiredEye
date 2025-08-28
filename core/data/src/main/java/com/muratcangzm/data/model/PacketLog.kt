package com.muratcangzm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packet_log")
data class PacketLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val uid: Int?,
    val packageName: String?,
    val protocol: String, // TCP/UDP/ICMP
    val localAddress: String,
    val localPort: Int,
    val remoteAddress: String,
    val remotePort: Int,
    val bytes: Long
)
