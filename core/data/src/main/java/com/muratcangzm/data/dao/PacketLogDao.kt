package com.muratcangzm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muratcangzm.data.model.PacketLog
import kotlinx.coroutines.flow.Flow

@Dao
interface PacketLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<PacketLog>)

    @Query(
        """
        SELECT * FROM packet_log
        WHERE timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun rangeFlow(from: Long, to: Long, limit: Int): Flow<List<PacketLog>>

    @Query(
        """
        SELECT * FROM packet_log
        WHERE timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun rangeOnce(from: Long, to: Long, limit: Int): List<PacketLog>

    @Query(
        """
        SELECT COALESCE(SUM(bytes), 0)
        FROM packet_log
        WHERE timestamp BETWEEN :from AND :to
        """
    )
    fun totalBytesBetweenFlow(from: Long, to: Long): Flow<Long>

    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dateLabel,
            COALESCE(SUM(bytes), 0) AS totalBytes
        FROM packet_log
        WHERE timestamp BETWEEN :from AND :to
        GROUP BY dateLabel
        ORDER BY dateLabel DESC
        LIMIT :limit
        """
    )
    fun bytesByDayFlow(from: Long, to: Long, limit: Int): Flow<List<PacketBytesByDayRow>>

    @Query(
        """
        SELECT
            uid AS uid,
            packageName AS packageName,
            COALESCE(SUM(bytes), 0) AS totalBytes
        FROM packet_log
        WHERE timestamp BETWEEN :from AND :to
        GROUP BY uid, packageName
        ORDER BY totalBytes DESC
        LIMIT 1
        """
    )
    fun topAppByBytesFlow(from: Long, to: Long): Flow<TopAppByBytesRow?>
}

data class PacketBytesByDayRow(
    val dateLabel: String,
    val totalBytes: Long
)

data class TopAppByBytesRow(
    val uid: Int?,
    val packageName: String?,
    val totalBytes: Long
)
