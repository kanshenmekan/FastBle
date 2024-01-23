package com.huyuhui.fastble.common

import kotlin.math.max

@Suppress("unused")
class BleConnectStrategy private constructor() {
    companion object {
        /**
         * 当存在mac相同的设备已经在连接的时候，忽略掉后面发起的连接，直至这次连接失败或者成功,已经存在连接成功，不会发起连接
         */
        const val CONNECT_BACKPRESSURE_DROP: Int = 0

        /**
         * 当存在mac相同的设备已经在连接的时候，取消之前的链接，直接用最新发起的，已经存在连接成功，不会发起连接
         */
        const val CONNECT_BACKPRESSURE_LAST: Int = 1
        const val DEFAULT_CONNECT_RETRY_COUNT = 0
        const val DEFAULT_CONNECT_RETRY_INTERVAL: Long = 2000
        const val DEFAULT_CONNECT_OVER_TIME: Long = 10000
    }

    var connectBackpressureStrategy = CONNECT_BACKPRESSURE_DROP
        private set
    /**
     * connect retry count
     */
    var reConnectCount = DEFAULT_CONNECT_RETRY_COUNT
        private set

    /**
     * connect retry interval
     */
    var reConnectInterval = DEFAULT_CONNECT_RETRY_INTERVAL
        private set

    /**
     * Get operate connect Over Time
     *
     */
    var connectOverTime: Long = DEFAULT_CONNECT_OVER_TIME
        private set


    class Builder {
        private var connectBackpressureStrategy = CONNECT_BACKPRESSURE_DROP
        private var reConnectCount = DEFAULT_CONNECT_RETRY_COUNT
        private var reConnectInterval = DEFAULT_CONNECT_RETRY_INTERVAL
        private var connectOverTime = DEFAULT_CONNECT_OVER_TIME

        fun setConnectBackpressureStrategy(backpressureStrategy: Int): Builder {
            this.connectBackpressureStrategy = backpressureStrategy
            return this
        }

        fun setReConnectCount(count: Int): Builder {
            reConnectCount = max(0, count)
            return this
        }

        fun setReConnectInterval(interval: Long): Builder {
            reConnectInterval = interval
            return this
        }

        fun setConnectOverTime(time: Long): Builder {
            connectOverTime = time
            return this
        }

        fun build(): BleConnectStrategy {
            val strategy = BleConnectStrategy()
            strategy.reConnectCount = this.reConnectCount
            strategy.reConnectInterval = this.reConnectInterval
            strategy.connectOverTime = this.connectOverTime
            strategy.connectBackpressureStrategy = strategy.connectBackpressureStrategy
            return strategy
        }
    }

    override fun toString(): String {
        return "BleConnectStrategy(connectBackpressureStrategy=$connectBackpressureStrategy, reConnectCount=$reConnectCount, reConnectInterval=$reConnectInterval, connectOverTime=$connectOverTime)"
    }


}