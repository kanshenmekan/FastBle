package com.hyh.ble.common


class BleConnectStrategy {
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
    var connectOverTime = DEFAULT_CONNECT_OVER_TIME
        set(value) {
            field = if (value <= 0) {
                100
            } else {
                value
            }
        }

    fun setReConnectCount(count: Int, interval: Long): BleConnectStrategy {
        reConnectCount = if (count > 10) 10 else count
        reConnectInterval = if (interval < 0) 0 else interval
        return this
    }

    fun applyStrategy(strategy: BleConnectStrategy) :BleConnectStrategy{
        this.connectBackpressureStrategy = strategy.connectBackpressureStrategy
        this.reConnectCount = strategy.reConnectCount
        this.reConnectInterval = strategy.reConnectInterval
        this.connectOverTime = strategy.connectOverTime
        return this
    }
}