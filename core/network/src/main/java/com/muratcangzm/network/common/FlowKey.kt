package com.muratcangzm.network.common

import android.util.LruCache
import java.util.concurrent.TimeUnit

data class FlowKey(
    val proto: Int,
    val aIp: String, val aPort: Int,
    val bIp: String, val bPort: Int
) {
    fun norm(): FlowKey =
        if (aIp < bIp || (aIp == bIp && aPort <= bPort)) this
        else FlowKey(proto, bIp, bPort, aIp, aPort)
}

 data class UidEntry(val uid: Int?, val expireAt: Long)

 val uidCache = object : LruCache<FlowKey, UidEntry>(8192) {}
 val UID_TTL_MS = TimeUnit.SECONDS.toMillis(30)