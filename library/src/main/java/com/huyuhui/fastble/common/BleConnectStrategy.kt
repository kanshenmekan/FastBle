package com.huyuhui.fastble.common

import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.RequiresApi
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

        const val DEFAULT_DISCOVER_SERVICE_TIMEOUT: Long = 5000
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

    /**
     * 发现服务超时时间
     */
    var discoverServiceTimeout = DEFAULT_DISCOVER_SERVICE_TIMEOUT
        private set

    /**
     * true 当发起连接的时候，无法找到设备，会保持连接状态，不回调结果（如果设置了超时，会一直等待到超时时间回调连接超时），等待设备可以连接之后，再回调结果。
     * false 直接连接，回调结果，默认为false
     */
    var mAutoConnect = false
        private set

    /**
     *
     * @see BluetoothDevice.TRANSPORT_AUTO
     */
    var transport: Int = 0
        private set

    /**
     * @see BluetoothDevice.PHY_LE_1M_MASK
     */
    var phy: Int = 1
        private set

    //设置默认的uuid写入
    var defaultWriteServiceUUID: String? = null
        private set

    var defaultWriteCharacteristicUUID: String? = null
        private set

    //设置默认Read的uuid
    var defaultReadServiceUUID: String? = null
        private set

    var defaultReadCharacteristicUUID: String? = null
        private set

    class Builder() {
        private var connectBackpressureStrategy = CONNECT_BACKPRESSURE_DROP
        private var reConnectCount = DEFAULT_CONNECT_RETRY_COUNT
        private var reConnectInterval = DEFAULT_CONNECT_RETRY_INTERVAL
        private var connectOverTime = DEFAULT_CONNECT_OVER_TIME

        private var discoverServiceTimeout = DEFAULT_DISCOVER_SERVICE_TIMEOUT
        private var mAutoConnect = false

        /**
         *
         * @see BluetoothDevice.TRANSPORT_AUTO
         */
        private var transport: Int = 0

        /**
         * @see BluetoothDevice.PHY_LE_1M_MASK
         */
        private var phy: Int = 1

        private var defaultWriteServiceUUID: String? = null
        private var defaultWriteCharacteristicUUID: String? = null

        private var defaultReadServiceUUID: String? = null
        private var defaultReadCharacteristicUUID: String? = null

        constructor(bleConnectStrategy: BleConnectStrategy) : this() {
            connectBackpressureStrategy = bleConnectStrategy.connectBackpressureStrategy
            reConnectCount = bleConnectStrategy.reConnectCount
            reConnectInterval = bleConnectStrategy.reConnectInterval
            connectOverTime = bleConnectStrategy.connectOverTime
            mAutoConnect = bleConnectStrategy.mAutoConnect
            transport = bleConnectStrategy.transport
            phy = bleConnectStrategy.phy
            discoverServiceTimeout = bleConnectStrategy.discoverServiceTimeout
            defaultWriteServiceUUID = bleConnectStrategy.defaultWriteServiceUUID
            defaultWriteCharacteristicUUID = bleConnectStrategy.defaultWriteCharacteristicUUID
            defaultReadServiceUUID = bleConnectStrategy.defaultReadServiceUUID
            defaultReadCharacteristicUUID = bleConnectStrategy.defaultReadCharacteristicUUID
        }

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

        fun setDiscoverServiceTimeout(discoverServiceTimeout: Long) = apply {
            this.discoverServiceTimeout = discoverServiceTimeout
        }

        fun setAutoConnect(autoConnect: Boolean): Builder {
            mAutoConnect = autoConnect
            return this
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun setTransport(transport: Int) = apply {
            this.transport = transport
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun setPhy(phy: Int) = apply {
            this.phy = phy
        }

        fun defaultWriteUUID(defaultServiceUUID: String, defaultCharacteristicUUID: String) =
            apply {
                this.defaultWriteServiceUUID = defaultServiceUUID
                this.defaultWriteCharacteristicUUID = defaultCharacteristicUUID
            }

        fun defaultReadUUID(defaultServiceUUID: String, defaultCharacteristicUUID: String) = apply {
            this.defaultReadServiceUUID = defaultServiceUUID
            this.defaultReadCharacteristicUUID = defaultCharacteristicUUID
        }

        fun build(): BleConnectStrategy {
            val strategy = BleConnectStrategy()
            strategy.reConnectCount = this.reConnectCount
            strategy.reConnectInterval = this.reConnectInterval
            strategy.connectOverTime = this.connectOverTime
            strategy.connectBackpressureStrategy = this.connectBackpressureStrategy
            strategy.mAutoConnect = this.mAutoConnect
            strategy.transport = this.transport
            strategy.phy = this.phy
            strategy.discoverServiceTimeout = this.discoverServiceTimeout
            strategy.defaultWriteServiceUUID = this.defaultWriteServiceUUID
            strategy.defaultWriteCharacteristicUUID = this.defaultWriteCharacteristicUUID
            strategy.defaultReadServiceUUID = this.defaultReadServiceUUID
            strategy.defaultReadCharacteristicUUID = this.defaultReadCharacteristicUUID
            return strategy
        }
    }

    override fun toString(): String {
        return "BleConnectStrategy(connectBackpressureStrategy=$connectBackpressureStrategy, reConnectCount=$reConnectCount, reConnectInterval=$reConnectInterval, connectOverTime=$connectOverTime, discoverServiceTimeout=$discoverServiceTimeout, mAutoConnect=$mAutoConnect, transport=$transport, phy=$phy)"
    }


}