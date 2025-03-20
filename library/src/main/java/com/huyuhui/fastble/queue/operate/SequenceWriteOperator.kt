package com.huyuhui.fastble.queue.operate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.bluetooth.BleOperator
import com.huyuhui.fastble.callback.BleWriteCallback
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.queue.TaskResult
import kotlinx.coroutines.channels.Channel
import java.lang.ref.WeakReference

const val PRIORITY_WRITE_DEFAULT = 500
const val DELAY_WRITE_DEFAULT: Long = 100

@SuppressLint("MissingPermission")
@Suppress("unused")
class SequenceWriteOperator private constructor(priority: Int, delay: Long) :
    SequenceBleOperator(priority, delay) {
    var serviceUUID: String? = null
        private set
    var characteristicUUID: String? = null
        private set
    var data: ByteArray? = null
        private set
    var bleWriteCallback: BleWriteCallback? = null
        private set
    var split: Boolean = true
        private set

    var splitNum: Int = BleManager.splitWriteNum
        private set
    var continueWhenLastFail: Boolean = false
        private set
    var intervalBetweenTwoPackage: Long = 0
        private set
    var writeType: Int = BleOperator.WRITE_TYPE_DEFAULT
        private set
    private var mContinuous = false
    private var mTimeout = 0L
    private var channelWeakReference: WeakReference<Channel<TaskResult>>? = null
    private val wrappedBleWriteCallback by lazy {
        object : BleWriteCallback() {
            override fun onWriteSuccess(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic,
                current: Int,
                total: Int,
                justWrite: ByteArray,
                data: ByteArray,
            ) {
                bleWriteCallback?.onWriteSuccess(
                    bleDevice,
                    characteristic,
                    current,
                    total,
                    justWrite,
                    data
                )
                if (current == total) {
                    channelWeakReference?.get()
                        ?.trySend(TaskResult(this@SequenceWriteOperator, true))
                }
            }

            override fun onWriteFailure(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic?,
                exception: BleException,
                current: Int,
                total: Int,
                justWrite: ByteArray?,
                data: ByteArray?,
                isTotalFail: Boolean,
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
                if (isTotalFail) {
                    channelWeakReference?.get()
                        ?.trySend(TaskResult(this@SequenceWriteOperator, false))
                }
            }

        }
    }

    override fun execute(bleDevice: BleDevice, channel: Channel<TaskResult>) {
        if (serviceUUID.isNullOrEmpty() || characteristicUUID.isNullOrEmpty()) {
            if (continuous) {
                channel.trySend(TaskResult(this, false))
            }
            return
        }
        if (continuous) {
            channelWeakReference = WeakReference(channel)
            BleManager.write(
                bleDevice,
                serviceUUID!!,
                characteristicUUID!!,
                callback = wrappedBleWriteCallback,
                writeType = writeType,
                data = data,
                split = split,
                splitNum = splitNum,
                continueWhenLastFail = continueWhenLastFail,
                intervalBetweenTwoPackage = intervalBetweenTwoPackage
            )
        } else {
            BleManager.write(
                bleDevice,
                serviceUUID!!,
                characteristicUUID!!,
                callback = bleWriteCallback,
                writeType = writeType,
                data = data,
                split = split,
                splitNum = splitNum,
                continueWhenLastFail = continueWhenLastFail,
                intervalBetweenTwoPackage = intervalBetweenTwoPackage
            )
        }
    }

    override val continuous: Boolean
        get() = mContinuous
    override val timeout: Long
        get() = mTimeout

    open class Builder() {
        private var priority: Int = PRIORITY_WRITE_DEFAULT
        private var delay: Long = DELAY_WRITE_DEFAULT
        private var serviceUUID: String? = null
        private var characteristicUUID: String? = null
        private var data: ByteArray? = null
        private var bleWriteCallback: BleWriteCallback? = null
        private var split: Boolean = true
        private var splitNum = BleManager.splitWriteNum
        private var continueWhenLastFail: Boolean = false
        private var intervalBetweenTwoPackage: Long = 0
        private var writeType: Int = BleOperator.WRITE_TYPE_DEFAULT
        private var continuous: Boolean = false
        private var timeout: Long = 0

        constructor(writeOperator: SequenceWriteOperator) : this() {
            this.priority = writeOperator.priority
            this.delay = writeOperator.delay
            this.serviceUUID = writeOperator.serviceUUID
            this.characteristicUUID = writeOperator.characteristicUUID
            this.data = writeOperator.data
            this.bleWriteCallback = writeOperator.bleWriteCallback
            this.split = writeOperator.split
            this.splitNum = writeOperator.splitNum
            this.continueWhenLastFail = writeOperator.continueWhenLastFail
            this.intervalBetweenTwoPackage = writeOperator.intervalBetweenTwoPackage
            this.writeType = writeOperator.writeType
            this.continuous = writeOperator.continuous
            this.timeout = writeOperator.timeout
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

        fun splitNum(splitNum: Int): Builder {
            this.splitNum = splitNum
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

        @SuppressLint("PrivateApi")
        fun applySequenceWriteOperator(writeOperator: SequenceWriteOperator) {
            writeOperator.serviceUUID = this.serviceUUID
            writeOperator.characteristicUUID = this.characteristicUUID
            writeOperator.data = this.data
            writeOperator.bleWriteCallback = this.bleWriteCallback
            writeOperator.split = this.split
            writeOperator.splitNum = this.splitNum
            writeOperator.continueWhenLastFail = this.continueWhenLastFail
            writeOperator.intervalBetweenTwoPackage = this.intervalBetweenTwoPackage
            writeOperator.writeType = this.writeType
            writeOperator.mContinuous = this.continuous
            writeOperator.mTimeout = this.timeout
        }

        fun build(): SequenceWriteOperator {
            return SequenceWriteOperator(priority, delay).apply {
                applySequenceWriteOperator(this)
            }
        }
    }
}