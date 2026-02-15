package com.muratcangzm.core.leak

class NativeLeakAnalyzer {

    external fun nativeInit(windowMs: Long)
    external fun nativeSetWindowMs(windowMs: Long)
    external fun nativeReset()
    external fun nativeOnDns(tsMs: Long, uid: Int, qname: String, qtype: Int, serverIp: String)
    external fun nativeSnapshotJson(topN: Int): String

    companion object {
        init {
            System.loadLibrary("wiredeye_native")
        }
    }
}
