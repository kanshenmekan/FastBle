package com.hyh.ble.callback

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException

abstract class BleGattCallback : BluetoothGattCallback() {

    abstract fun onStartConnect(bleDevice: BleDevice)

    abstract fun onConnectFail(bleDevice: BleDevice?, exception: BleException?)
    abstract fun onConnectCancel(bleDevice: BleDevice?,skip:Boolean)
    abstract fun onConnectSuccess(bleDevice: BleDevice?, gatt: BluetoothGatt?, status: Int)

    abstract fun onDisConnected(
        isActiveDisConnected: Boolean,
        device: BleDevice?,
        gatt: BluetoothGatt?,
        status: Int
    )
}