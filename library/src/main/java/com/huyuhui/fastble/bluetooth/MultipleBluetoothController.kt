package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.utils.BleLruHashMap
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
@Suppress("unused")
internal class MultipleBluetoothController {
    //保存已经连接成功的设备
    private val connectedDevicesMap: BleLruHashMap<String, BleBluetooth> by lazy {
        BleLruHashMap(BleManager.maxConnectCount)
    }

    //保存正在连接的设备
    private val connectingDevicesMap: ConcurrentHashMap<String, BleBluetooth> = ConcurrentHashMap()

    fun buildConnectingBle(bleDevice: BleDevice): BleBluetooth {
        return connectingDevicesMap.putIfAbsent(bleDevice.key, BleBluetooth(bleDevice))
            ?: connectingDevicesMap[bleDevice.key]!!
    }


    fun removeConnectingBle(bleBluetooth: BleBluetooth?) {
        bleBluetooth?.let { connectingDevicesMap.remove(it.deviceKey) }
    }

    fun addConnectedBleBluetooth(bleBluetooth: BleBluetooth?) {
        if (bleBluetooth == null) return
        if (!connectedDevicesMap.containsKey(bleBluetooth.deviceKey)) {
            connectedDevicesMap[bleBluetooth.deviceKey] = bleBluetooth
        }
    }

    fun removeConnectedBleBluetooth(bleBluetooth: BleBluetooth?) {
        if (bleBluetooth == null) {
            return
        }
        if (connectedDevicesMap.containsKey(bleBluetooth.deviceKey)) {
            connectedDevicesMap.remove(bleBluetooth.deviceKey)
        }
    }

    fun isConnecting(bleDevice: BleDevice?): Boolean {
        return bleDevice != null && connectingDevicesMap.containsKey(bleDevice.key)
    }

    fun cancelConnecting(bleDevice: BleDevice?, skip: Boolean) {
        bleDevice?.key?.let { key ->
            connectingDevicesMap.remove(key)?.let { bleBluetooth ->
                // 回调和销毁在锁外执行，不阻塞并发操作
                bleBluetooth.bleGattCallback?.onConnectCancel(bleBluetooth.bleDevice, skip)
                bleBluetooth.destroy()
            }
        }
    }

    fun cancelOrDisconnect(bleDevice: BleDevice?) {
        cancelConnecting(bleDevice, false)
        disconnect(bleDevice)
    }

    fun cancelAllConnectingDevice() {
        connectingDevicesMap.values.forEach { bleBluetooth ->
            bleBluetooth.bleGattCallback?.onConnectCancel(bleBluetooth.bleDevice, false)
            bleBluetooth.destroy()
        }
        connectingDevicesMap.clear() // 清空所有连接中设备
    }

    fun isConnectedDevice(bleDevice: BleDevice?): Boolean {
        return bleDevice != null && connectedDevicesMap.containsKey(bleDevice.key)
    }

    fun isConnectedDevice(bluetoothDevice: BluetoothDevice?): Boolean {
        return bluetoothDevice != null && isConnectedDevice(
            BleManager.convertBleDevice(
                bluetoothDevice
            )
        )
    }

    fun getConnectedBleBluetooth(bleDevice: BleDevice?): BleBluetooth? {
        return bleDevice?.key?.let { connectedDevicesMap[it] }
    }

    fun disconnect(bleDevice: BleDevice?) {
        if (isConnectedDevice(bleDevice)) {
            getConnectedBleBluetooth(bleDevice)?.disconnect()
        }
    }

    fun disconnectAllDevice() {
        val keys = ArrayList(connectedDevicesMap.keys)
        keys.forEach { key ->
            connectedDevicesMap[key]?.disconnect()
        }
    }

    fun destroy() {
        // 处理已连接设备
        val connectedKeys = ArrayList(connectedDevicesMap.keys)
        connectedKeys.forEach { key ->
            connectedDevicesMap.remove(key)?.destroy() // 原子操作：移除并销毁
        }

        // 处理连接中设备
        val connectingKeys = ArrayList(connectingDevicesMap.keys)
        connectingKeys.forEach { key ->
            connectingDevicesMap.remove(key)?.destroy() // 原子操作
        }
    }

    fun getConnectedBleBluetoothList(): List<BleBluetooth> {
        return connectedDevicesMap.values.toList()
    }

    fun getConnectedDeviceList(): List<BleDevice> {
        refreshConnectedDevice()
        return getConnectedBleBluetoothList().map { it.bleDevice }
    }

    private fun getConnectingBleBluetoothList(): List<BleBluetooth> {
        return connectingDevicesMap.values.toList()
    }

    fun getConnectingDeviceList(): List<BleDevice> {
        return connectingDevicesMap.values.toList().map { it.bleDevice }
    }

    private fun refreshConnectedDevice() {
        val bluetoothList = getConnectedBleBluetoothList()
        bluetoothList.forEach { bleBluetooth ->
            if (BleManager.getConnectState(bleBluetooth.bleDevice) != BluetoothProfile.STATE_CONNECTED) {
                removeConnectedBleBluetooth(bleBluetooth)
                bleBluetooth.destroy()
            }
        }
    }

    fun onBleOff() {
        getConnectedBleBluetoothList().forEach {
            removeConnectedBleBluetooth(it)
            it.bleGattCallback?.onDisConnected(
                true,
                it.bleDevice,
                it.bluetoothGatt,
                BluetoothProfile.STATE_DISCONNECTED
            )
            it.destroy()
        }
        connectedDevicesMap.clear()
        getConnectingBleBluetoothList().forEach {
            removeConnectingBle(it)
            it.bleGattCallback?.onConnectFail(
                it.bleDevice,
                BleException.OtherException(
                    BleException.BLUETOOTH_NOT_ENABLED,
                    "Bluetooth is not enabled"
                )
            )
            it.destroy()
        }
        connectingDevicesMap.clear()
    }
}