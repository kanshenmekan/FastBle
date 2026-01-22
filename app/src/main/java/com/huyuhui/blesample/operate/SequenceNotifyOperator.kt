package com.huyuhui.blesample.operate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.callback.BleNotifyCallback
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.queue.operate.DELAY_WRITE_DEFAULT
import com.huyuhui.fastble.queue.operate.PRIORITY_WRITE_DEFAULT
import com.huyuhui.fastble.queue.operate.SequenceBleOperator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("MissingPermission")
@Suppress("unused")
class SequenceNotifyOperator private constructor(priority: Int) :
    SequenceBleOperator(priority) {
    var serviceUUID: String? = null
        private set
    var characteristicUUID: String? = null
        private set
    var bleNotifyCallback: BleNotifyCallback? = null
        private set
    var useCharacteristicDescriptor: Boolean = false
        private set
    var continuous = false
        private set
    var timeout = 0L
        private set
    var delay: Long = 0L
        private set

    private suspend fun notifyForResult(bleDevice: BleDevice): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val wrappedBleNotifyCallback = object : BleNotifyCallback() {
            override fun onNotifySuccess(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic,
            ) {
                bleNotifyCallback?.onNotifySuccess(bleDevice, characteristic)
                deferred.complete(true)
            }

            override fun onNotifyFailure(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic?,
                exception: BleException,
            ) {
                bleNotifyCallback?.onNotifyFailure(bleDevice, characteristic, exception)
                deferred.complete(false)
            }

            override fun onNotifyCancel(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic,
            ) {
                bleNotifyCallback?.onNotifyCancel(bleDevice, characteristic)
            }

            override fun onCharacteristicChanged(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic,
                data: ByteArray,
            ) {
                bleNotifyCallback?.onCharacteristicChanged(bleDevice, characteristic, data)
            }
        }
        BleManager.notify(
            bleDevice,
            serviceUUID!!,
            characteristicUUID!!,
            callback = wrappedBleNotifyCallback,
            useCharacteristicDescriptor = useCharacteristicDescriptor
        )
        return deferred.await()
    }

    override suspend fun execute(bleDevice: BleDevice) {
        if (serviceUUID.isNullOrEmpty() || characteristicUUID.isNullOrEmpty()) {
            return
        }
        if (continuous) {
            if (timeout > 0) {
                withTimeoutOrNull(timeout) {
                    notifyForResult(bleDevice)
                }
            } else {
                notifyForResult(bleDevice)
            }
        } else {
            BleManager.notify(
                bleDevice,
                serviceUUID!!,
                characteristicUUID!!,
                callback = bleNotifyCallback,
                useCharacteristicDescriptor = useCharacteristicDescriptor
            )
        }
        delay(delay)
    }

    class Builder() {
        private var priority: Int = PRIORITY_WRITE_DEFAULT
        private var delay: Long = DELAY_WRITE_DEFAULT
        private var serviceUUID: String? = null
        private var characteristicUUID: String? = null
        private var bleNotifyCallback: BleNotifyCallback? = null
        private var continuous: Boolean = false
        private var timeout: Long = 0
        private var useCharacteristicDescriptor = false

        constructor(notifyOperator: SequenceNotifyOperator) : this() {
            this.priority = notifyOperator.priority
            this.delay = notifyOperator.delay
            this.serviceUUID = notifyOperator.serviceUUID
            this.characteristicUUID = notifyOperator.characteristicUUID
            this.continuous = notifyOperator.continuous
            this.timeout = notifyOperator.timeout
            this.useCharacteristicDescriptor = notifyOperator.useCharacteristicDescriptor
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

        fun bleNotifyCallback(bleNotifyCallback: BleNotifyCallback?): Builder {
            this.bleNotifyCallback = bleNotifyCallback
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
        fun applySequenceNotifyOperator(notifyOperator: SequenceNotifyOperator) {
            notifyOperator.serviceUUID = this.serviceUUID
            notifyOperator.characteristicUUID = this.characteristicUUID
            notifyOperator.bleNotifyCallback = this.bleNotifyCallback
            notifyOperator.continuous = this.continuous
            notifyOperator.timeout = this.timeout
            notifyOperator.useCharacteristicDescriptor = this.useCharacteristicDescriptor
            notifyOperator.delay = delay
        }

        fun build(): SequenceNotifyOperator {
            return SequenceNotifyOperator(priority).apply {
                applySequenceNotifyOperator(this)
            }
        }
    }
}