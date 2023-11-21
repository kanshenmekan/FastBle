package com.huyuhui.fastble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException

abstract class BleRssiCallback : BleOperateCallback() {
    final override fun onTimeOutFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        data: ByteArray?
    ) {
        onRssiFailure(bleDevice, exception)
    }

    abstract fun onRssiFailure(bleDevice: BleDevice, exception: BleException)

    abstract fun onRssiSuccess(bleDevice: BleDevice, rssi: Int)
}