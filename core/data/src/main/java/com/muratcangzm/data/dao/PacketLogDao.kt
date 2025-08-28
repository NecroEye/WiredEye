package com.muratcangzm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.muratcangzm.data.model.PacketLog

@Dao
interface PacketLogDao {
    @Insert
    fun insertAll(items: List<PacketLog>)

    @Query("SELECT * FROM packet_log WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC LIMIT :limit")
    fun range(from: Long, to: Long, limit: Int): List<PacketLog>
}