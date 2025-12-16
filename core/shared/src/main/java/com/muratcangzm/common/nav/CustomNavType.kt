package com.muratcangzm.common.nav

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.json.Json

inline fun <reified T : Any> createGenericNavType(isNullableAllowed: Boolean = false): NavType<T?> {
    return object : NavType<T?>(isNullableAllowed) {
        override fun get(bundle: Bundle, key: String): T? {
            return bundle.getString(key)?.let {
                Json.decodeFromString(it)
            }
        }

        override fun parseValue(value: String): T? {
            return if (value == "null") null else Json.decodeFromString(Uri.decode(value))
        }

        override fun serializeAsValue(value: T?): String {
            return if (value == null) "null" else Uri.encode(Json.encodeToString(value))
        }

        override fun put(bundle: Bundle, key: String, value: T?) {
            if (value == null && isNullableAllowed) {
                bundle.putString(key, null)
            } else {
                bundle.putString(key, value?.let { Json.encodeToString(it) })
            }
        }
    }
}


inline fun <reified T : Any> createGenericNavTypeList(): NavType<List<T>> {
    return object : NavType<List<T>>(false) {

        override fun get(bundle: Bundle, key: String): List<T> {
            val jsonString = bundle.getString(key)
            return if (jsonString.isNullOrEmpty()) {
                emptyList()
            } else {
                Json.decodeFromString(jsonString)
            }
        }

        override fun parseValue(value: String): List<T> {
            return Json.decodeFromString(Uri.decode(value))
        }

        override fun serializeAsValue(value: List<T>): String {
            return Uri.encode(Json.encodeToString(value))
        }

        override fun put(bundle: Bundle, key: String, value: List<T>) {
            bundle.putString(key, Json.encodeToString(value))
        }
    }
}

