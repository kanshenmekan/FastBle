package com.hyh.ble.queue.operate

import android.annotation.SuppressLint
import com.hyh.ble.BleManager
import com.hyh.ble.bluetooth.BleBluetooth
import com.hyh.ble.bluetooth.BleOperator
import com.hyh.ble.callback.BleWriteCallback

const val PRIORITY_WRITE_DEFAULT = 1
const val DELAY_WRITE_DEFAULT: Long = 100

@SuppressLint("MissingPermission")
class SequenceWriteOperator private constructor(priority: Int, delay: Long) :
    SequenceBleOperator(priority, delay) {
    private var serviceUUID: String? = null
    private var characteristicUUID: String? = null
    private var data: ByteArray? = null
    private var bleWriteCallback: BleWriteCallback? = null
    private var split: Boolean = true
    private var continueWhenLastFail: Boolean = false
    private var intervalBetweenTwoPackage: Long = 0
    private var writeType: Int = BleOperator.WRITE_TYPE_DEFAULT
    override fun execute(bleBluetooth: BleBluetooth) {
        if (serviceUUID.isNullOrEmpty() || characteristicUUID.isNullOrEmpty()) {
            return
        }
        BleManager.write(
            bleBluetooth.bleDevice,
            serviceUUID!!,
            characteristicUUID!!,
            callback = bleWriteCallback,
            writeType = writeType,
            data = data,
            split = split,
            continueWhenLastFail = continueWhenLastFail,
            intervalBetweenTwoPackage = intervalBetweenTwoPackage
        )
    }

    class Builder() {
        private var priority: Int = PRIORITY_WRITE_DEFAULT
        private var delay: Long = DELAY_WRITE_DEFAULT
        private var serviceUUID: String? = null
        private var characteristicUUID: String? = null
        private var data: ByteArray? = null
        private var bleWriteCallback: BleWriteCallback? = null
        private var split: Boolean = true
        private var continueWhenLastFail: Boolean = false
        private var intervalBetweenTwoPackage: Long = 0
        private var writeType: Int = BleOperator.WRITE_TYPE_DEFAULT

        constructor(builder: Builder) : this() {
            this.priority = builder.priority
            this.delay = builder.delay
            this.serviceUUID = builder.serviceUUID
            this.characteristicUUID = builder.characteristicUUID
            this.data = builder.data
            this.bleWriteCallback = builder.bleWriteCallback
            this.split = builder.split
            this.continueWhenLastFail = builder.continueWhenLastFail
            this.intervalBetweenTwoPackage = builder.intervalBetweenTwoPackage
            this.writeType = builder.writeType
        }

        fun serviceUUID(serviceUUID: String): Builder {
            this.serviceUUID = serviceUUID
            return this
        }

        fun characteristicUUID(characteristicUUID: String): Builder {
            this.characteristicUUID = characteristicUUID
            return this
        }

        fun data(data: ByteArray?): Builder {
            this.data = data
            return this
        }

        fun bleWriteCallback(bleWriteCallback: BleWriteCallback?): Builder {
            this.bleWriteCallback = bleWriteCallback
            return this
        }

        fun split(split: Boolean): Builder {
            this.split = split
            return this
        }

        fun continueWhenLastFail(continueWhenLastFail: Boolean): Builder {
            this.continueWhenLastFail = continueWhenLastFail
            return this
        }

        fun intervalBetweenTwoPackage(intervalBetweenTwoPackage: Long): Builder {
            this.intervalBetweenTwoPackage = intervalBetweenTwoPackage
            return this
        }

        fun writeType(writeType: Int): Builder {
            this.writeType = writeType
            return this
        }

        fun build(): SequenceWriteOperator {
            return SequenceWriteOperator(priority, delay).apply {
                this.serviceUUID = this@Builder.serviceUUID
                this.characteristicUUID = this@Builder.characteristicUUID
                this.data = this@Builder.data
                this.bleWriteCallback = this@Builder.bleWriteCallback
                this.split = this@Builder.split
                this.continueWhenLastFail = this@Builder.continueWhenLastFail
                this.intervalBetweenTwoPackage = this@Builder.intervalBetweenTwoPackage
                this.writeType = this@Builder.writeType
            }
        }
    }
}