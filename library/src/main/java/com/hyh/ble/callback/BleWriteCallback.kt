package com.hyh.ble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.hyh.ble.data.BleDevice
import com.hyh.ble.data.BleWriteState
import com.hyh.ble.exception.BleException

abstract class BleWriteCallback : BleOperateCallback() {

    abstract fun onWriteSuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        current: Int = BleWriteState.DATA_WRITE_SINGLE,
        total: Int = BleWriteState.DATA_WRITE_SINGLE,
        justWrite: ByteArray,
        data: ByteArray = justWrite
    )

    abstract fun onWriteFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        current: Int = BleWriteState.DATA_WRITE_SINGLE,
        total: Int = BleWriteState.DATA_WRITE_SINGLE,
        justWrite: ByteArray?,
        data: ByteArray? = justWrite,
        isTotalFail: Boolean = true
    )

    final override fun onTimeOutFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        data: ByteArray?
    ) {
        onWriteFailure(bleDevice, characteristic, exception, justWrite = data)
    }
}