package per.goweii.androidserver.runtime.utils

import com.google.gson.Gson

internal object JsonUtils {
    private val gson = Gson()

    fun <T> fromJson(json: String, type: Class<T>): T {
        return gson.fromJson(json, type)
    }

    fun <T> toJson(any: T): String {
        return gson.toJson(any)
    }
}