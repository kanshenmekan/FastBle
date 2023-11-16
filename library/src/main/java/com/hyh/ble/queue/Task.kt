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

    /**
     * 当continuous 为true的时候，等待任务完成之后（触发回调或者超时,才会进行delay任务，之后获取下一个任务
     * 为false的时候，会直接进行delay任务，之后获取下一个任务
     * @see timeout
     */
    var continuous: Boolean = false

    /**
     * @see continuous
     * 当continuous为true之后，这个设置才有效果，如果任务在时间内没有回调，直接忽略掉，进行delay任务，之后获取下一个任务
     * 建议给一个适当的时长，以便任务有足够时间触发回调
     * 如果timeout为0，则会一直等待，直到任务回调触发
     */
    var timeout:Long = 0
}