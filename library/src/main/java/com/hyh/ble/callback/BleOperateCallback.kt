package com.hyh.ble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException

abstract class BleOperateCallback {
    abstract fun onTimeOutFailure(
        bleDevice: BleDevice?,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException?,
        data: ByteArray?
    )
}