package com.muratcangzm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_event")
data class DnsEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val uid: Int?,
    val packageName: String?,
    val qname: String, // queried hostname
    val qtype: String, // A/AAAA/CNAME/etc.
    val server: String
)