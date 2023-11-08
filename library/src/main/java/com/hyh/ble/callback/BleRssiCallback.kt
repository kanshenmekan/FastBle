package com.hyh.ble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException

abstract class BleRssiCallback : BleOperateCallback() {
    final override fun onTimeOutFailure(
        bleDevice: BleDevice?,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException?,
        data: ByteArray?
    ) {
        onRssiFailure(bleDevice, exception)
    }

    abstract fun onRssiFailure(bleDevice: BleDevice?, exception: BleException?)

    abstract fun onRssiSuccess(bleDevice: BleDevice?, rssi: Int)
}