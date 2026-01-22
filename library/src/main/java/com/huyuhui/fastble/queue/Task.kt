package com.huyuhui.fastble.queue

import java.util.concurrent.atomic.AtomicLong

abstract class Task(val priority: Int) {
    companion object {
        private val atomic = AtomicLong(0)
    }

    var sequenceNum: Long = 0
        private set

    init {
        sequenceNum = atomic.getAndIncrement()
    }

}