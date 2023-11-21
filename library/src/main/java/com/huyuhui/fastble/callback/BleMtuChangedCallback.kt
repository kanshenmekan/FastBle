package com.huyuhui.fastble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException

abstract class BleMtuChangedCallback : BleOperateCallback() {

    abstract fun onSetMTUFailure(bleDevice: BleDevice, exception: BleException)

    abstract fun onMtuChanged(bleDevice: BleDevice, mtu: Int)

    final override fun onTimeOutFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        data: ByteArray?
    ) {
        onSetMTUFailure(bleDevice, exception)
    }
}