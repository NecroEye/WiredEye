package com.muratcangzm.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muratcangzm.data.dao.DnsEventDao
import com.muratcangzm.data.dao.PacketLogDao
import com.muratcangzm.data.model.DnsEvent
import com.muratcangzm.data.model.PacketLog

@Database(
    entities = [PacketLog::class, DnsEvent::class],
    version = 1,
    exportSchema = true
)
abstract class WiredEyeDatabase : RoomDatabase() {

    abstract fun packetLogDao(): PacketLogDao
    abstract fun dnsEventDao(): DnsEventDao
}
