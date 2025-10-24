package com.muratcangzm.preferences

interface PreferencesRepository {
    suspend fun isStartHintShown(): Boolean
    suspend fun setStartHintShown()
}