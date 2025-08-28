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

    override fun labelFor(uid: Int): String? {
        nameCache.get(uid)?.let { return it }
        val pkg = packageFor(uid) ?: return null
        val ai: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
        val label = pm.getApplicationLabel(ai)?.toString()
        if (!label.isNullOrBlank()) nameCache.put(uid, label)
        return label
    }

    override fun packageFor(uid: Int): String? {
        pkgCache.get(uid)?.let { return it }
        val list = pm.getPackagesForUid(uid) ?: return null
        val pkg = list.firstOrNull()
        if (!pkg.isNullOrBlank()) pkgCache.put(uid, pkg)
        return pkg
    }
}
