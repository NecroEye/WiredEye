package com.muratcangzm.preferences

import androidx.datastore.core.DataStore
import com.muratcangzm.preferences.model.Preferences
import kotlinx.coroutines.flow.first

class PreferenceRepositoryImp(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    override suspend fun isStartHintShown(): Boolean {
        val prefs = dataStore.data.first()
        return prefs.startHintShown
    }

    override suspend fun setStartHintShown() {
        dataStore.updateData { current ->
            current.copy(startHintShown = true)
        }
    }
}