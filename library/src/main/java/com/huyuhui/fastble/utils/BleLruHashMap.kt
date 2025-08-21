package com.huyuhui.fastble.utils

import com.huyuhui.fastble.bluetooth.BleBluetooth
import kotlin.math.ceil

internal class BleLruHashMap<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(
    ceil(maxSize / 0.75).toInt() + 1, 0.75f, true
) {

    // 内部锁对象，保护所有操作的线程安全
    private val lock = Any()

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
        if (size > maxSize && eldest.value is BleBluetooth) {
            BleLog.w("The number of connections has surpassed the maximum limit.")
            (eldest.value as BleBluetooth).disconnect()
        }
        return size > maxSize
    }

    override val size: Int
        get() = synchronized(lock) { super.size }

    override fun isEmpty(): Boolean = synchronized(lock) { super.isEmpty() }

    override fun containsKey(key: K): Boolean = synchronized(lock) { super.containsKey(key) }

    override fun containsValue(value: V): Boolean = synchronized(lock) { super.containsValue(value) }

    override fun get(key: K): V? = synchronized(lock) { super.get(key) }

    override fun put(key: K, value: V): V? = synchronized(lock) { super.put(key, value) }

    override fun remove(key: K): V? = synchronized(lock) { super.remove(key) }

    override fun putAll(from: Map<out K, V>) = synchronized(lock) { super.putAll(from) }

    override fun clear() = synchronized(lock) { super.clear() }

    override fun toString(): String = synchronized(lock) {
        val sb = StringBuilder()
        for ((key, value) in entries) {
            sb.append(String.format("%s:%s ", key, value))
        }
        sb.toString()
    }

    override val keys: MutableSet<K>
        get() = synchronized(lock) { super.keys }
    // 新增：获取不可变的键集合（避免外部迭代修改）

    override val values: MutableCollection<V>
        get() = synchronized(lock) { super.values }

}