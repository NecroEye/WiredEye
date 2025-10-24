package com.muratcangzm.preferences.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.muratcangzm.preferences.PreferenceRepositoryImp
import com.muratcangzm.preferences.PreferencesRepository
import com.muratcangzm.preferences.PreferencesSerializer
import com.muratcangzm.preferences.model.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val PREFS_FILE = "ghostly_prefs.json"

val prefsModule = module {
    single<DataStore<Preferences>> {
        val context: Context = androidContext()
        DataStoreFactory.create(
            serializer = PreferencesSerializer,
            produceFile = { context.dataStoreFile(PREFS_FILE) },
            corruptionHandler = ReplaceFileCorruptionHandler { com.muratcangzm.preferences.model.Preferences() },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        )
    }

    single<PreferencesRepository> { PreferenceRepositoryImp(get()) }
}