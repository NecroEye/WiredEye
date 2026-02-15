package com.muratcangzm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muratcangzm.data.common.TopHost
import com.muratcangzm.data.model.DnsEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: DnsEvent)

    @Query(
        """
        SELECT * FROM dns_event
        WHERE timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun rangeFlow(from: Long, to: Long, limit: Int): Flow<List<DnsEvent>>

    @Query(
        """
        SELECT * FROM dns_event
        WHERE timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun rangeOnce(from: Long, to: Long, limit: Int): List<DnsEvent>

    @Query(
        """
    SELECT qname AS host, COUNT(*) AS c
    FROM dns_event
    WHERE timestamp BETWEEN :from AND :to
    GROUP BY qname
    ORDER BY c DESC
    LIMIT :limit
    """
    )
    fun topHostsFlow(from: Long, to: Long, limit: Int): Flow<List<TopHost>>

    @Query(
        """
    SELECT qname AS host, COUNT(*) AS c
    FROM dns_event
    WHERE timestamp BETWEEN :from AND :to
    GROUP BY qname
    ORDER BY c DESC
    LIMIT :limit
    """
    )
    suspend fun topHostsOnce(from: Long, to: Long, limit: Int): List<TopHost>

    @Query(
        """
        SELECT COALESCE(COUNT(*), 0)
        FROM dns_event
        WHERE timestamp BETWEEN :from AND :to
        """
    )
    fun countBetweenFlow(from: Long, to: Long): Flow<Int>

    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dateLabel,
            COALESCE(COUNT(*), 0) AS leakCount
        FROM dns_event
        WHERE timestamp BETWEEN :from AND :to
        GROUP BY dateLabel
        ORDER BY dateLabel DESC
        LIMIT :limit
        """
    )
    fun countByDayFlow(from: Long, to: Long, limit: Int): Flow<List<DnsCountByDayRow>>
}

data class DnsCountByDayRow(
    val dateLabel: String,
    val leakCount: Int
)
