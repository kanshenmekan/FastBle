package com.hyh.ble.queue

import java.util.concurrent.atomic.AtomicLong

open class Task(val priority: Int, var delay: Long) {
    companion object {
        private val atomic = AtomicLong(0)
    }

    var sequenceNum: Long = 0
        private set

    init {
        sequenceNum = atomic.getAndIncrement()
    }
}