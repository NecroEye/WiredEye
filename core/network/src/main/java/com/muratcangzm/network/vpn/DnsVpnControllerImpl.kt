package com.muratcangzm.network.vpn

import android.content.Context
import android.content.Intent

class DnsVpnControllerImpl(private val ctx: Context) : DnsVpnController {
    override fun start(context: Context) {
        val i = Intent(context, DnsSnifferVpnService::class.java)
            .setAction(DnsSnifferVpnService.ACTION_START)
        context.startForegroundService(i)
    }

    override fun stop(context: Context) {
        val i = Intent(context, DnsSnifferVpnService::class.java)
            .setAction(DnsSnifferVpnService.ACTION_STOP)
        context.startService(i)
    }
}
