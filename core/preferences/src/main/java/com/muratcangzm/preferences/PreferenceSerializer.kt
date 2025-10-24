package com.muratcangzm.preferences

import androidx.datastore.core.Serializer
import com.muratcangzm.preferences.model.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object PreferencesSerializer : Serializer<Preferences> {

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override val defaultValue: Preferences
        get() = Preferences()

    override suspend fun readFrom(input: InputStream): Preferences = withContext(Dispatchers.IO) {
        return@withContext try {
            val text = input.readBytes().decodeToString()
            if (text.isBlank()) defaultValue else json.decodeFromString(text)
        } catch (t: Throwable) {
            t.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            val encoded = json.encodeToString(t)
            output.write(encoded.encodeToByteArray())
        }
    }
}