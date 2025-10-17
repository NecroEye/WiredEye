package com.muratcangzm.monitor.ui.adapters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.muratcangzm.monitor.common.UiPacket
import kotlin.math.max

data class UiPacketItem(val id: String, val model: UiPacket, val contentHash: Int)

class UiPacketAdapter {
    val items = mutableStateListOf<UiPacketItem>()
    fun submit(models: List<UiPacket>) {
        val seen = HashMap<String, Int>(max(16, models.size))
        val newProjected = ArrayList<UiPacketItem>(models.size)
        val oldById = HashMap<String, UiPacketItem>(items.size)
        items.forEach { oldById[it.id] = it }
        for (m in models) {
            val n = (seen[m.key] ?: 0) + 1
            seen[m.key] = n
            val id = "${m.key}#$n"
            val ch = fastContentHash(m)
            val reused = oldById[id]?.takeIf { it.contentHash == ch }
            if (reused != null) newProjected.add(reused) else newProjected.add(UiPacketItem(id, m, ch))
        }
        val newIds = newProjected.mapTo(HashSet(newProjected.size)) { it.id }
        for (i in items.lastIndex downTo 0) if (!newIds.contains(items[i].id)) items.removeAt(i)
        var i = 0
        while (i < newProjected.size) {
            val desired = newProjected[i]
            if (i >= items.size) items.add(desired) else {
                val current = items[i]
                if (current.id == desired.id) {
                    if (current.contentHash != desired.contentHash || current.model !== desired.model) items[i] = desired
                } else {
                    val existingIndex = items.indexOfFirst { it.id == desired.id }
                    if (existingIndex >= 0) {
                        val moved = items.removeAt(existingIndex)
                        items.add(i, moved)
                        if (moved.contentHash != desired.contentHash || moved.model !== desired.model) items[i] = desired
                    } else items.add(i, desired)
                }
            }
            i++
        }
        while (items.size > newProjected.size) items.removeAt(items.lastIndex)
    }
    private fun fastContentHash(m: UiPacket): Int {
        var h = 17
        h = 31 * h + (m.raw.timestamp xor (m.raw.timestamp ushr 32)).toInt()
        h = 31 * h + (m.raw.bytes xor (m.raw.bytes ushr 32)).toInt()
        h = 31 * h + m.proto.hashCode()
        h = 31 * h + m.from.hashCode()
        h = 31 * h + m.to.hashCode()
        h = 31 * h + m.app.hashCode()
        return h
    }
}

@Composable
fun rememberUiPacketAdapter(): UiPacketAdapter = remember { UiPacketAdapter() }