package com.hyh.ble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException

abstract class BleNotifyCallback : BleOperateCallback() {
    abstract fun onNotifySuccess(bleDevice: BleDevice, characteristic: BluetoothGattCharacteristic)

    abstract fun onNotifyFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException?
    )

    abstract fun onNotifyCancel(bleDevice: BleDevice, characteristic: BluetoothGattCharacteristic?)
    abstract fun onCharacteristicChanged(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    )

    final override fun onTimeOutFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        data: ByteArray?
    ) {
        onNotifyFailure(bleDevice, characteristic, exception)
    }
}