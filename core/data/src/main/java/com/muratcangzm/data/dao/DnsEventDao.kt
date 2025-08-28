package com.muratcangzm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.muratcangzm.data.common.TopHost
import com.muratcangzm.data.model.DnsEvent


@Dao
interface DnsEventDao {
    @Insert
    fun insert(event: DnsEvent)

    @Query("SELECT * FROM dns_event WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC LIMIT :limit")
    fun range(from: Long, to: Long, limit: Int): List<DnsEvent>

    @Query("SELECT qname, COUNT(*) as c FROM dns_event WHERE timestamp BETWEEN :from AND :to GROUP BY qname ORDER BY c DESC LIMIT :limit")
    fun topHosts(from: Long, to: Long, limit: Int): List<TopHost>
}