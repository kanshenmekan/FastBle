package com.huyuhui.blesample.operate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.bluetooth.BleBluetooth
import com.huyuhui.fastble.callback.BleNotifyCallback
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.queue.TaskResult
import com.huyuhui.fastble.queue.operate.DELAY_WRITE_DEFAULT
import com.huyuhui.fastble.queue.operate.PRIORITY_WRITE_DEFAULT
import com.huyuhui.fastble.queue.operate.SequenceBleOperator
import kotlinx.coroutines.channels.Channel
import java.lang.ref.WeakReference

@SuppressLint("MissingPermission")
@Suppress("unused")
class SequenceNotifyOperator private constructor(priority: Int, delay: Long) :
    SequenceBleOperator(priority, delay) {
    var serviceUUID: String? = null
        private set
    var characteristicUUID: String? = null
        private set
    var bleNotifyCallback: BleNotifyCallback? = null
        private set
    var useCharacteristicDescriptor: Boolean = false
        private set
    private var mContinuous = false
    private var mTimeout = 0L
    private var channelWeakReference: WeakReference<Channel<TaskResult>>? = null
    private val wrappedBleNotifyCallback = object : BleNotifyCallback() {
        override fun onNotifySuccess(
            bleDevice: BleDevice,
            characteristic: BluetoothGattCharacteristic,
        ) {
            bleNotifyCallback?.onNotifySuccess(bleDevice, characteristic)
            channelWeakReference?.get()
                ?.trySend(TaskResult(this@SequenceNotifyOperator, true))
        }

        override fun onNotifyFailure(
            bleDevice: BleDevice,
            characteristic: BluetoothGattCharacteristic?,
            exception: BleException,
        ) {
            bleNotifyCallback?.onNotifyFailure(bleDevice, characteristic, exception)
            channelWeakReference?.get()
                ?.trySend(TaskResult(this@SequenceNotifyOperator, false))
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

    override fun execute(bleBluetooth: BleBluetooth, channel: Channel<TaskResult>) {
        if (serviceUUID.isNullOrEmpty() || characteristicUUID.isNullOrEmpty()) {
            if (continuous) {
                channel.trySend(TaskResult(this, false))
            }
            return
        }
        if (continuous) {
            channelWeakReference = WeakReference(channel)
            BleManager.notify(
                bleBluetooth.bleDevice,
                serviceUUID!!,
                characteristicUUID!!,
                callback = wrappedBleNotifyCallback,
                useCharacteristicDescriptor = useCharacteristicDescriptor
            )
        } else {
            BleManager.notify(
                bleBluetooth.bleDevice,
                serviceUUID!!,
                characteristicUUID!!,
                callback = bleNotifyCallback,
                useCharacteristicDescriptor = useCharacteristicDescriptor
            )
        }
    }

    override val continuous: Boolean
        get() = mContinuous
    override val timeout: Long
        get() = mTimeout

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
            notifyOperator.mContinuous = this.continuous
            notifyOperator.mTimeout = this.timeout
            notifyOperator.useCharacteristicDescriptor = this.useCharacteristicDescriptor
        }

        fun build(): SequenceNotifyOperator {
            return SequenceNotifyOperator(priority, delay).apply {
                applySequenceNotifyOperator(this)
            }
        }
    }
}