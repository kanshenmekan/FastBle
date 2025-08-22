package com.huyuhui.fastble.callback

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException

abstract class BleGattCallback : BluetoothGattCallback() {

    abstract fun onStartConnect(bleDevice: BleDevice)

    abstract fun onConnectFail(bleDevice: BleDevice?, exception: BleException)

    /**
     * @param skip true表示 当前发起的连接，设备已经连接，或者该连接被新发起的连接覆盖 false 则表示手动取消了该次连接
     */
    open fun onConnectCancel(bleDevice: BleDevice, skip: Boolean) {}
    abstract fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt?, status: Int)

    abstract fun onDisConnected(
        isActiveDisConnected: Boolean,
        device: BleDevice,
        gatt: BluetoothGatt?,
        status: Int
    )

    open fun onServicesDiscovered(bleDevice: BleDevice, gatt: BluetoothGatt?, status: Int) {

    }

    open fun onPhyUpdate(
        bleDevice: BleDevice,
        gatt: BluetoothGatt?,
        txPhy: Int,
        rxPhy: Int,
        status: Int
    ) {

    }

    open fun onPhyRead(
        bleDevice: BleDevice,
        gatt: BluetoothGatt?,
        txPhy: Int,
        rxPhy: Int,
        status: Int
    ) {

    }

}