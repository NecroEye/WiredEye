package com.muratcangzm.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.muratcangzm.data.dao.DnsEventDao
import com.muratcangzm.data.dao.PacketLogDao
import com.muratcangzm.data.model.DnsEvent
import com.muratcangzm.data.model.PacketLog

@Database(entities = [PacketLog::class, DnsEvent::class], version = 1)
abstract class WiredEyeDatabase : RoomDatabase() {
    abstract fun packetLogDao(): PacketLogDao
    abstract fun dnsEventDao(): DnsEventDao

    companion object {
        fun build(ctx: Context) = Room.databaseBuilder(ctx, WiredEyeDatabase::class.java, "monitor.db").build()
    }
}