package com.huyuhui.fastble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException

abstract class BleNotifyCallback : BleOperateCallback() {
    abstract fun onNotifySuccess(bleDevice: BleDevice, characteristic: BluetoothGattCharacteristic)

    abstract fun onNotifyFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException
    )

    abstract fun onNotifyCancel(bleDevice: BleDevice, characteristic: BluetoothGattCharacteristic)
    abstract fun onCharacteristicChanged(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    )

}