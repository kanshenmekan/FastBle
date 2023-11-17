package com.hyh.ble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException

abstract class BleReadCallback : BleOperateCallback() {
    final override fun onTimeOutFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        data: ByteArray?
    ) {
        onReadFailure(bleDevice, characteristic, exception)
    }

    abstract fun onReadSuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    )

    abstract fun onReadFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException?
    )
}