package com.huyuhui.fastble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException

abstract class BleReadCallback : BleOperateCallback() {
    abstract fun onReadSuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    )

    abstract fun onReadFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException
    )
}