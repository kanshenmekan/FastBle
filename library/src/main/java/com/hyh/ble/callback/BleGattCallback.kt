package com.hyh.ble.callback

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException

abstract class BleGattCallback : BluetoothGattCallback() {

    abstract fun onStartConnect(bleDevice: BleDevice)

    abstract fun onConnectFail(bleDevice: BleDevice?, exception: BleException?)

    /**
     * @param skip true表示 当前发起的连接，设备已经连接，或者该连接被新发起的连接覆盖 false 则表示手动取消了该次连接
     */
    abstract fun onConnectCancel(bleDevice: BleDevice?,skip:Boolean)
    abstract fun onConnectSuccess(bleDevice: BleDevice?, gatt: BluetoothGatt?, status: Int)

    abstract fun onDisConnected(
        isActiveDisConnected: Boolean,
        device: BleDevice?,
        gatt: BluetoothGatt?,
        status: Int
    )
}