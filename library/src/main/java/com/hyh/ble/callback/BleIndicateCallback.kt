package com.hyh.ble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException

abstract class BleIndicateCallback : BleOperateCallback() {
    abstract fun onIndicateSuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic
    )

    abstract fun onIndicateFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException?
    )

    abstract fun onIndicateCancel(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?
    )

    abstract fun onCharacteristicChanged(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray?
    )

    final override fun onTimeOutFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        data: ByteArray?
    ) {
        onIndicateFailure(bleDevice, characteristic, exception)
    }
}