package com.muratcangzm.data.di

import androidx.room.Room
import com.muratcangzm.common.di.DispatchersQualifiers
import com.muratcangzm.data.dao.DnsEventDao
import com.muratcangzm.data.dao.PacketLogDao
import com.muratcangzm.data.db.WiredEyeDatabase
import com.muratcangzm.data.helper.PmUidResolver
import com.muratcangzm.data.helper.UidResolver
import com.muratcangzm.data.repo.packetRepo.PacketRepository
import com.muratcangzm.data.repo.packetRepo.PacketRepositoryImpl
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            WiredEyeDatabase::class.java,
            "monitor.db"
        ).build()
    }

    single<PacketLogDao> { get<WiredEyeDatabase>().packetLogDao() }
    single<DnsEventDao> { get<WiredEyeDatabase>().dnsEventDao() }

    single<UidResolver> { PmUidResolver(androidContext().packageManager) }

    single<PacketRepository> {
        val dao = get<PacketLogDao>()
        val dnsDao = get<DnsEventDao>()
        val io = get<CoroutineDispatcher>(DispatchersQualifiers.IO)
        PacketRepositoryImpl(
            packetDao = dao,
            dnsDao = dnsDao,
            ioDispatcher = io
        )
    }
}
