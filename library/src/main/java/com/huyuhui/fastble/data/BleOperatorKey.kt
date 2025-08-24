package com.huyuhui.fastble.data

class BleOperatorKey(uuidService: String, uuidCharacteristic: String) {
    private val normalizedServiceUuid = uuidService.lowercase()
    private val normalizedCharacteristicUuid = uuidCharacteristic.lowercase()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleOperatorKey

        if (normalizedServiceUuid != other.normalizedServiceUuid) return false
        if (normalizedCharacteristicUuid != other.normalizedCharacteristicUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = normalizedServiceUuid.hashCode()
        result = 31 * result + normalizedCharacteristicUuid.hashCode()
        return result
    }

}