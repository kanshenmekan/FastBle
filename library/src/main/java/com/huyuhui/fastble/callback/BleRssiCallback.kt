package com.huyuhui.fastble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException

abstract class BleRssiCallback : BleOperateCallback() {

    abstract fun onRssiFailure(bleDevice: BleDevice, exception: BleException)

    abstract fun onRssiSuccess(bleDevice: BleDevice, rssi: Int)
}