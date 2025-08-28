package com.muratcangzm.data.helper

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.LruCache

interface UidResolver {
    fun labelFor(uid: Int): String?
    fun packageFor(uid: Int): String?
}

class PmUidResolver(private val pm: PackageManager) : UidResolver {
    private val nameCache = LruCache<Int, String>(256)
    private val pkgCache = LruCache<Int, String>(256)
    private val negative = LruCache<Int, Boolean>(256)

    override fun packageFor(uid: Int): String? {
        if (negative.get(uid) == true) return null
        pkgCache.get(uid)?.let { return it }
        val pkg = runCatching { pm.getPackagesForUid(uid)?.firstOrNull() }.getOrNull()
        return if (!pkg.isNullOrBlank()) {
            pkgCache.put(uid, pkg); pkg
        } else {
            negative.put(uid, true); null
        }
    }

    override fun labelFor(uid: Int): String? {
        nameCache.get(uid)?.let { return it }
        val pkg = packageFor(uid) ?: return null
        val label = runCatching {
            val ai: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai)?.toString()
        }.getOrNull()
        return if (!label.isNullOrBlank()) {
            nameCache.put(uid, label); label
        } else null
    }
}
