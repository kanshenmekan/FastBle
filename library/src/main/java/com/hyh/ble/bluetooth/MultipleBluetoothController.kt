package com.hyh.ble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.hyh.ble.BleManager
import com.hyh.ble.data.BleDevice
import com.hyh.ble.utils.BleLruHashMap

@SuppressLint("MissingPermission")
class MultipleBluetoothController {
    //保存已经连接成功的设备
    private val bleLruHashMap: BleLruHashMap<String, BleBluetooth> =
        BleLruHashMap(BleManager.maxConnectCount)

    //保存正在连接的设备
    private val bleTempHashMap: HashMap<String, BleBluetooth> = HashMap()

    @Synchronized
    fun buildConnectingBle(bleDevice: BleDevice): BleBluetooth {
        val bleBluetooth = BleBluetooth(bleDevice)
        if (!bleTempHashMap.containsKey(bleBluetooth.deviceKey)) {
            bleTempHashMap[bleBluetooth.deviceKey] = bleBluetooth
        }
        return bleBluetooth
    }

    @Synchronized
    fun removeConnectingBle(bleBluetooth: BleBluetooth?) {
        if (bleBluetooth == null) {
            return
        }
        if (bleTempHashMap.containsKey(bleBluetooth.deviceKey)) {
            bleTempHashMap.remove(bleBluetooth.deviceKey)
        }
    }

    @Synchronized
    fun addBleBluetooth(bleBluetooth: BleBluetooth?) {
        if (bleBluetooth == null) {
            return
        }
        if (!bleLruHashMap.containsKey(bleBluetooth.deviceKey)) {
            bleLruHashMap[bleBluetooth.deviceKey] = bleBluetooth
        }
    }

    @Synchronized
    fun removeBleBluetooth(bleBluetooth: BleBluetooth?) {
        if (bleBluetooth == null) {
            return
        }
        if (bleLruHashMap.containsKey(bleBluetooth.deviceKey)) {
            bleLruHashMap.remove(bleBluetooth.deviceKey)
        }
    }

    fun isConnecting(bleDevice: BleDevice?): Boolean {
        return bleDevice != null && bleTempHashMap.containsKey(bleDevice.key)
    }
    @Synchronized
    fun cancelConnecting(bleDevice: BleDevice?) {
        bleTempHashMap[bleDevice?.key]?.destroy()
        bleTempHashMap.remove(bleDevice?.key)
    }
    fun cancelAllConnectingDevice(){
        bleLruHashMap.clear()
        for (entry: Map.Entry<String?, BleBluetooth> in bleTempHashMap) {
            entry.value.destroy()
        }
        bleTempHashMap.clear()
    }
    @Synchronized
    fun isContainDevice(bleDevice: BleDevice?): Boolean {
        return bleDevice != null && bleLruHashMap.containsKey(bleDevice.key)
    }

    @Synchronized
    fun isContainDevice(bluetoothDevice: BluetoothDevice?): Boolean {
        return bluetoothDevice != null && bleLruHashMap.containsKey(bluetoothDevice.name + bluetoothDevice.address)
    }

    @Synchronized
    fun getBleBluetooth(bleDevice: BleDevice?): BleBluetooth? {
        if (bleDevice != null) {
            if (bleLruHashMap.containsKey(bleDevice.key)) {
                return bleLruHashMap[bleDevice.key]
            }
        }
        return null
    }

    @Synchronized
    fun disconnect(bleDevice: BleDevice?) {
        if (isContainDevice(bleDevice)) {
            getBleBluetooth(bleDevice)?.disconnect()
        }
    }

    @Synchronized
    fun disconnectAllDevice() {
        for (stringBleBluetoothEntry: Map.Entry<String?, BleBluetooth> in bleLruHashMap) {
            stringBleBluetoothEntry.value.disconnect()
        }
        bleLruHashMap.clear()
    }

    @Synchronized
    fun destroy() {
        for (entry: Map.Entry<String?, BleBluetooth> in bleLruHashMap) {
            entry.value.destroy()
        }
        bleLruHashMap.clear()
        for (entry: Map.Entry<String?, BleBluetooth> in bleTempHashMap) {
            entry.value.destroy()
        }
        bleTempHashMap.clear()
    }

    @Synchronized
    fun getBleBluetoothList(): List<BleBluetooth> {
        return bleLruHashMap.values.toList()
    }

    @Synchronized
    fun getDeviceList(): List<BleDevice> {
        refreshConnectedDevice()
        return getBleBluetoothList().map { it.bleDevice }
    }

    private fun refreshConnectedDevice() {
        val bluetoothList = getBleBluetoothList()
        for (bleBluetooth in bluetoothList) {
            if (!BleManager.isConnected(bleBluetooth.bleDevice)) {
                removeBleBluetooth(bleBluetooth)
            }
        }
    }
}