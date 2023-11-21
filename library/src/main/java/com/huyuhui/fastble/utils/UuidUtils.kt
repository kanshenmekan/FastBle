package com.huyuhui.fastble.utils

object UuidUtils {
    private const val base_uuid_regex =
        "0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb"
    private const val baseUUID = "0000xxxx-0000-1000-8000-00805F9B34FB"

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

    fun uuid128To16(uuid: String, lower_case: Boolean = true): String? {
        return if (isBaseUUID(uuid)) {
            if (lower_case) uuid.substring(4, 8).lowercase()
            else uuid.substring(4, 8).uppercase()
        } else null
    }

    fun uuid16To128(uuid: String, lower_case: Boolean = true): String? {
        return if (is16UUID(uuid)) {
            if (lower_case) baseUUID.replaceRange(4, 8, uuid)
                .lowercase() else baseUUID.replaceRange(4, 8, uuid).uppercase()
        } else null
    }
}