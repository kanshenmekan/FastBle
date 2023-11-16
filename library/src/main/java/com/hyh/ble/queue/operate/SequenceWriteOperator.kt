package com.hyh.ble.queue.operate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import com.hyh.ble.BleManager
import com.hyh.ble.bluetooth.BleBluetooth
import com.hyh.ble.bluetooth.BleOperator
import com.hyh.ble.callback.BleWriteCallback
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException
import com.hyh.ble.queue.TaskResult
import kotlinx.coroutines.channels.Channel
import java.lang.ref.WeakReference

const val PRIORITY_WRITE_DEFAULT = 500
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
    private var channelWeakReference: WeakReference<Channel<TaskResult>>? = null
    private val wrappedBleWriteCallback by lazy {
        object : BleWriteCallback() {
            override fun onWriteSuccess(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic,
                current: Int,
                total: Int,
                justWrite: ByteArray?,
                data: ByteArray?
            ) {
                bleWriteCallback?.onWriteSuccess(
                    bleDevice,
                    characteristic,
                    current,
                    total,
                    justWrite,
                    data
                )
                channelWeakReference?.get()?.trySend(TaskResult(this@SequenceWriteOperator, true))
            }

            override fun onWriteFailure(
                bleDevice: BleDevice?,
                characteristic: BluetoothGattCharacteristic?,
                exception: BleException?,
                current: Int,
                total: Int,
                justWrite: ByteArray?,
                data: ByteArray?,
                isTotalFail: Boolean
            ) {
                bleWriteCallback?.onWriteFailure(
                    bleDevice,
                    characteristic,
                    exception,
                    current,
                    total,
                    justWrite,
                    data,
                    isTotalFail
                )
                channelWeakReference?.get()?.trySend(TaskResult(this@SequenceWriteOperator, false))
            }

        }
    }

    override fun execute(bleBluetooth: BleBluetooth, channel: Channel<TaskResult>) {
        if (serviceUUID.isNullOrEmpty() || characteristicUUID.isNullOrEmpty()) {
            if (continuous) {
                channel.trySend(TaskResult(this, false))
            }
            return
        }
        if (continuous) {
            channelWeakReference = WeakReference(channel)
            BleManager.write(
                bleBluetooth.bleDevice,
                serviceUUID!!,
                characteristicUUID!!,
                callback = wrappedBleWriteCallback,
                writeType = writeType,
                data = data,
                split = split,
                continueWhenLastFail = continueWhenLastFail,
                intervalBetweenTwoPackage = intervalBetweenTwoPackage
            )
        } else {
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
        private var continuous: Boolean = false
        private var timeout: Long = 0

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
            this.continuous = builder.continuous
            this.timeout = builder.timeout
        }

        fun priority(priority: Int): Builder {
            this.priority = priority
            return this
        }

        fun delay(delay: Long): Builder {
            this.delay = delay
            return this
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

        fun continuous(continuous: Boolean): Builder {
            this.continuous = continuous
            return this
        }

        fun timeout(timeout: Long): Builder {
            this.timeout = timeout
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
                this.continuous = this@Builder.continuous
                this.timeout = this@Builder.timeout
            }
        }
    }
}