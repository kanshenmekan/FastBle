package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleCoroutineExceptionHandler
import com.huyuhui.fastble.utils.BleLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import java.util.UUID

@SuppressLint("MissingPermission")
internal abstract class BleOperator(
    protected val bleBluetooth: BleBluetooth,
    val timeout: Long
) :
    CoroutineScope by CoroutineScope(
        SupervisorJob(bleBluetooth.coroutineContext.job) + Dispatchers.Main
                + BleCoroutineExceptionHandler { _, throwable ->
            BleLog.e(
                "Bluetooth operation: a coroutine error has occurred. ${throwable.message}\n " +
                        "Device:${bleBluetooth.bleDevice} \n"
            )
        }) {
    companion object {
        @JvmStatic
        val UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR =
            "00002902-0000-1000-8000-00805f9b34fb"


        const val WRITE_TYPE_DEFAULT = -1
    }

    /**
     * 操作的数据
     */
    val bleDevice: BleDevice
        get() = bleBluetooth.bleDevice


    abstract fun onTimeout(task: TimeoutTask, e: Throwable?, isActive: Boolean)

    protected val timeOutTask = TimeoutTask(
        timeout, object : TimeoutTask.OnResultCallBack {
            override fun onError(task: TimeoutTask, e: Throwable?, isActive: Boolean) {
                super.onError(task, e, isActive)
                onTimeout(task, e, isActive)
            }
        }
    )
    protected val mBluetoothGatt: BluetoothGatt?
        get() = bleBluetooth.bluetoothGatt
    var mGattService: BluetoothGattService? = null
        private set
    var mCharacteristic: BluetoothGattCharacteristic? = null
        private set


    private fun <T : BleOperator> withUUID(serviceUUID: UUID, characteristicUUID: UUID): T {
        mGattService = mBluetoothGatt?.getService(serviceUUID)
        mCharacteristic = mGattService?.getCharacteristic(characteristicUUID)
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    fun <T : BleOperator> withUUIDString(serviceUUID: String, characteristicUUID: String): T {
        return withUUID(fromUUID(serviceUUID), fromUUID(characteristicUUID))
    }

    protected fun fromUUID(uuid: String): UUID {
        return UUID.fromString(uuid)
    }

    fun removeTimeOut() {
        timeOutTask.success()
    }

    open fun destroy() {
        timeOutTask.onTimeoutResultCallBack = null
        cancel()
    }
}