package com.muratcangzm.network.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService

interface DnsVpnController {
    fun isPrepared(context: Context): Boolean = VpnService.prepare(context) == null
    fun prepareIntent(context: Context): Intent? = VpnService.prepare(context)
    fun start(context: Context)
    fun stop(context: Context)
}
