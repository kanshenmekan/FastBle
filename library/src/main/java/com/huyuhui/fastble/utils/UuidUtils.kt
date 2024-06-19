package com.huyuhui.fastble.utils

import android.annotation.SuppressLint

@Suppress("unused")
object UuidUtils {
    private const val BASE_UUID_REGEX =
        "0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb"
    private const val BASE_UUID = "0000xxxx-0000-1000-8000-00805F9B34FB"

    @SuppressLint("PrivateApi")
    fun isBaseUUID(uuid: String): Boolean {
        return uuid.lowercase()
            .matches(Regex("0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb"))
    }

    fun is16UUID(uuid: String): Boolean {
        return uuid.matches(Regex("""^[0-9a-fA-F]{4}$"""))
    }

    fun isUUID(str: String): Boolean {
        val uuidPattern = Regex(
            """^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"""
        )
        return uuidPattern.matches(str)
    }

    fun uuid128To16(uuid: String, lowerCase: Boolean = true): String? {
        return if (isBaseUUID(uuid)) {
            if (lowerCase) uuid.substring(4, 8).lowercase()
            else uuid.substring(4, 8).uppercase()
        } else null
    }

    fun uuid16To128(uuid: String, lowerCase: Boolean = true): String? {
        return if (is16UUID(uuid)) {
            if (lowerCase) BASE_UUID.replaceRange(4, 8, uuid)
                .lowercase() else BASE_UUID.replaceRange(4, 8, uuid).uppercase()
        } else null
    }
}