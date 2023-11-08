package com.hyh.ble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException

abstract class BleMtuChangedCallback : BleOperateCallback() {

    abstract fun onSetMTUFailure(bleDevice: BleDevice?, exception: BleException?)

    abstract fun onMtuChanged(bleDevice: BleDevice?, mtu: Int)

    final override fun onTimeOutFailure(
        bleDevice: BleDevice?,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException?,
        data: ByteArray?
    ) {
        onSetMTUFailure(bleDevice, exception)
    }
}