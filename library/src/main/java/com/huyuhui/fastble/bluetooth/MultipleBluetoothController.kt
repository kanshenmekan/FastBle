package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.utils.BleLruHashMap

@SuppressLint("MissingPermission")
class MultipleBluetoothController {
    //保存已经连接成功的设备
    private val bleLruHashMap: BleLruHashMap<String, BleBluetooth> =
        BleLruHashMap(BleManager.maxConnectCount)

    //保存正在连接的设备
    private val bleTempHashMap: HashMap<String, BleBluetooth> = HashMap()

    @Synchronized
    fun buildConnectingBle(bleDevice: BleDevice): BleBluetooth {
        return if (bleTempHashMap.containsKey(bleDevice.key)) {
            bleTempHashMap[bleDevice.key]!!
        } else {
            val bleBluetooth = BleBluetooth(bleDevice)
            bleTempHashMap[bleBluetooth.deviceKey] = bleBluetooth
            bleBluetooth
        }
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
    fun addConnectedBleBluetooth(bleBluetooth: BleBluetooth?) {
        if (bleBluetooth == null) {
            return
        }
        if (!bleLruHashMap.containsKey(bleBluetooth.deviceKey)) {
            bleLruHashMap[bleBluetooth.deviceKey] = bleBluetooth
        }
    }

    @Synchronized
    fun removeConnectedBleBluetooth(bleBluetooth: BleBluetooth?) {
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
    fun cancelConnecting(bleDevice: BleDevice?, skip: Boolean) {
        bleTempHashMap.remove(bleDevice?.key)?.let {
            it.bleGattCallback?.onConnectCancel(bleDevice!!, skip)
            it.destroy()
        }
    }

    @Synchronized
    fun cancelAllConnectingDevice() {
        for (entry: Map.Entry<String?, BleBluetooth> in bleTempHashMap) {
            cancelConnecting(entry.value.bleDevice, false)
        }
        bleTempHashMap.clear()
    }

    @Synchronized
    fun isConnectedDevice(bleDevice: BleDevice?): Boolean {
        return bleDevice != null && bleLruHashMap.containsKey(bleDevice.key)
    }

    @Synchronized
    fun isConnectedDevice(bluetoothDevice: BluetoothDevice?): Boolean {
        return bluetoothDevice != null && isConnectedDevice(
            BleManager.convertBleDevice(
                bluetoothDevice
            )
        )
    }

    @Synchronized
    fun getConnectedBleBluetooth(bleDevice: BleDevice?): BleBluetooth? {
        if (bleDevice != null) {
            if (bleLruHashMap.containsKey(bleDevice.key)) {
                return bleLruHashMap[bleDevice.key]
            }
        }
        return null
    }

    @Synchronized
    fun disconnect(bleDevice: BleDevice?) {
        if (isConnectedDevice(bleDevice)) {
            getConnectedBleBluetooth(bleDevice)?.disconnect()
        }
    }

    @Synchronized
    fun disconnectAllDevice() {
        for (stringBleBluetoothEntry: Map.Entry<String?, BleBluetooth> in bleLruHashMap) {
            stringBleBluetoothEntry.value.disconnect()
        }
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
    fun getConnectedBleBluetoothList(): List<BleBluetooth> {
        return bleLruHashMap.values.toList()
    }

    @Synchronized
    fun getConnectedDeviceList(): List<BleDevice> {
        refreshConnectedDevice()
        return getConnectedBleBluetoothList().map { it.bleDevice }
    }

    private fun getConnectingBleBluetoothList(): List<BleBluetooth> {
        return bleTempHashMap.values.toList()
    }

    fun getConnectingDeviceList(): List<BleDevice> {
        return bleTempHashMap.values.toList().map { it.bleDevice }
    }

    private fun refreshConnectedDevice() {
        val bluetoothList = getConnectedBleBluetoothList()
        for (bleBluetooth in bluetoothList) {
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
                BluetoothGatt.GATT_SUCCESS
            )
            it.destroy()
        }
        bleLruHashMap.clear()
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
        bleTempHashMap.clear()
    }
}