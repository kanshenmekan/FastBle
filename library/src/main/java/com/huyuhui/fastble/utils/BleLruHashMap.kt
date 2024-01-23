package com.huyuhui.fastble.utils

import com.huyuhui.fastble.bluetooth.BleBluetooth
import kotlin.math.ceil

internal class BleLruHashMap<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(
    ceil(maxSize / 0.75).toInt() + 1, 0.75f, true
) {

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
        if (size > maxSize && eldest.value is BleBluetooth) {
            BleLog.w("The number of connections has surpassed the maximum limit.")
            (eldest.value as BleBluetooth).disconnect()
        }
        return size > maxSize
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for ((key, value) in entries) {
            sb.append(String.format("%s:%s ", key, value))
        }
        return sb.toString()
    }
}