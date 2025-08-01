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
@Suppress("unused")
internal class MultipleBluetoothController {
    //保存已经连接成功的设备
    private val bleLruHashMap: BleLruHashMap<String, BleBluetooth> by lazy {
        BleLruHashMap(BleManager.maxConnectCount)
    }

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
            it.bleGattCallback?.onConnectCancel(it.bleDevice, skip)
            it.destroy()
        }
    }

    fun cancelOrDisconnect(bleDevice: BleDevice?) {
        cancelConnecting(bleDevice, false)
        disconnect(bleDevice)
    }

    @Synchronized
    fun cancelAllConnectingDevice() {
        val iterator = bleTempHashMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.bleGattCallback?.onConnectCancel(entry.value.bleDevice, false)
            entry.value.destroy() // 清理资源
            iterator.remove()    //通过迭代器安全移除
        }
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
        val keys = ArrayList(bleLruHashMap.keys)
        keys.forEach { key ->
            bleLruHashMap[key]?.disconnect()
        }
    }

    @Synchronized
    fun destroy() {
        // 处理已连接设备
        val connectedKeys = ArrayList(bleLruHashMap.keys)
        connectedKeys.forEach { key ->
            bleLruHashMap.remove(key)?.destroy() // 原子操作：移除并销毁
        }

        // 处理连接中设备
        val connectingKeys = ArrayList(bleTempHashMap.keys)
        connectingKeys.forEach { key ->
            bleTempHashMap.remove(key)?.destroy() // 原子操作
        }
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