package com.huyuhui.fastble.queue.operate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.bluetooth.BleOperator
import com.huyuhui.fastble.callback.BleWriteCallback
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

const val PRIORITY_WRITE_DEFAULT = 500
const val DELAY_WRITE_DEFAULT: Long = 100

@SuppressLint("MissingPermission")
@Suppress("unused")
class SequenceWriteOperator private constructor(priority: Int) :
    SequenceBleOperator(priority) {
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

    var delay: Long = 0
        private set

    /**
     * 当continuous 为true的时候，等待任务完成之后（触发回调或者超时）,才会进行delay任务，之后获取下一个任务
     * 为false的时候，会直接进行delay任务，之后获取下一个任务
     * @see timeout
     */
    var continuous = false
        private set

    //在队列中的超时时间

    /**
     * @see continuous
     * 当continuous为true之后，这个设置才有效果，如果任务在时间内没有回调，直接忽略掉，进行delay任务，之后获取下一个任务
     * 建议给一个适当的时长，以便任务有足够时间触发回调
     * 如果timeout为0，则会一直等待，直到任务回调触发
     */
    var timeout = 0L
        private set

    //这个写入过程的超时时间
    var operateTimeout = BleManager.operateTimeout
        private set

    private suspend fun writeDataForResult(bleDevice: BleDevice, data: ByteArray?): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val wrappedBleWriteCallback = object : BleWriteCallback() {
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
                    deferred.complete(true)
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
                    deferred.complete(false)
                }
            }
        }
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
            intervalBetweenTwoPackage = intervalBetweenTwoPackage,
            timeout = operateTimeout
        )
        return deferred.await()
    }

    override suspend fun execute(bleDevice: BleDevice) {
        if (serviceUUID.isNullOrEmpty() || characteristicUUID.isNullOrEmpty()) {
            bleWriteCallback?.onWriteFailure(
                bleDevice, characteristic = null, exception = BleException.OtherException(
                    BleException.ERROR_CODE_GATT, "gatt null"
                ), justWrite = null, data = data
            )
            delay(delay)
            return
        }
        if (continuous) {
            if (timeout > 0) {
                withTimeoutOrNull(timeout) {
                    writeDataForResult(bleDevice, data)
                }
            } else {
                writeDataForResult(bleDevice, data)
            }
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
                intervalBetweenTwoPackage = intervalBetweenTwoPackage,
                timeout = operateTimeout
            )
        }
        delay(delay)
    }

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

        private var operateTimeout: Long = BleManager.operateTimeout

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
            this.operateTimeout = writeOperator.operateTimeout
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

        fun operateTimeout(operateTimeout: Long) = apply {
            this.operateTimeout = operateTimeout
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
            writeOperator.continuous = this.continuous
            writeOperator.delay = this.delay
            writeOperator.timeout = this.timeout
            writeOperator.operateTimeout = this.operateTimeout
        }

        fun build(): SequenceWriteOperator {
            return SequenceWriteOperator(priority).apply {
                applySequenceWriteOperator(this)
            }
        }
    }
}