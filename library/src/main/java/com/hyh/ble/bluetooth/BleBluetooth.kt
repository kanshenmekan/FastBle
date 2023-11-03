package com.hyh.ble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import com.hyh.ble.BleManager
import com.hyh.ble.callback.BleGattCallback
import com.hyh.ble.callback.BleIndicateCallback
import com.hyh.ble.callback.BleMtuChangedCallback
import com.hyh.ble.callback.BleNotifyCallback
import com.hyh.ble.callback.BleReadCallback
import com.hyh.ble.callback.BleRssiCallback
import com.hyh.ble.callback.BleWriteCallback
import com.hyh.ble.common.TimeoutTask
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException
import com.hyh.ble.utils.BleLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BleBluetooth(val bleDevice: BleDevice) : CoroutineScope by MainScope() {
    private val connectTimeOutTask = TimeoutTask(
        BleManager.connectOverTime, object : TimeoutTask.OnResultCallBack {
            override fun onError(task: TimeoutTask, e: Throwable?, isActive: Boolean) {
                super.onError(task, e, isActive)
                connectedFail(BleException.TimeoutException())
            }
        }
    )
    var bleGattCallback: BleGattCallback? = null
        @Synchronized
        set

    private val bleNotifyOperatorMap: HashMap<String, BleOperator> = HashMap()
    private val bleIndicateOperatorMap: HashMap<String, BleOperator> = HashMap()
    private val bleWriteOperatorMap: HashMap<String, BleOperator> = HashMap()
    private val bleReadOperatorMap: HashMap<String, BleOperator> = HashMap()
    private var bleRssiOperator: BleOperator? = null
    private var bleMtuOperator: BleOperator? = null
    private var lastState: LastState? = null
    private var isActiveDisconnect = false
    var bluetoothGatt: BluetoothGatt? = null
        private set
    private var currentConnectRetryCount = 0
    val deviceKey
        get() = bleDevice.key

    @Synchronized
    fun connect(
        context: Context,
        autoConnect: Boolean,
        callback: BleGattCallback
    ): BluetoothGatt? {
        bleGattCallback = callback
        currentConnectRetryCount = 0
        if (BleManager.multipleBluetoothController.isContainConnectedDevice(bleDevice)) {
            return bluetoothGatt
        }
        return connect(context, autoConnect, currentConnectRetryCount)
    }

    @Synchronized
    private fun connect(
        context: Context,
        autoConnect: Boolean,
        connectRetryCount: Int
    ): BluetoothGatt? {
        ensureActive()
        BleLog.i(
            """
                connect device: ${bleDevice.name}
                mac: ${bleDevice.mac}
                autoConnect: $autoConnect
                currentThread: ${Thread.currentThread().id}
                connectCount:${connectRetryCount + 1}
                """.trimIndent()
        )
        lastState = LastState.CONNECT_CONNECTING
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bleDevice.device?.connectGatt(
                context, autoConnect, coreGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            bleDevice.device?.connectGatt(context, autoConnect, coreGattCallback)
        }
        if (bluetoothGatt != null) {
            if (connectRetryCount == 0) {
                bleGattCallback?.onStartConnect(bleDevice)
            }
            launch(Dispatchers.Main.immediate) {
                connectTimeOutTask.start()
            }
        } else {
            connectedFail(BleException.OtherException("GATT connect exception occurred!"))
        }
        return bluetoothGatt
    }

    private fun connectedFail(exception: BleException) {
        launch {
            disconnectGatt()
            refreshDeviceCache()
            closeBluetoothGatt()
            lastState = LastState.CONNECT_FAILURE
            BleManager.multipleBluetoothController.removeConnectingBle(this@BleBluetooth)
            bleGattCallback?.onConnectFail(
                bleDevice,
                exception
            )
        }
    }

    fun newOperator(uuid_service: String, uuid_characteristic: String): BleOperator {
        return BleOperator(this).withUUIDString(uuid_service, uuid_characteristic)
    }

    fun newOperator(): BleOperator {
        return BleOperator(this)
    }

    private fun discoverFail() {
        connectedFail(BleException.DiscoverException())
    }

    private fun connectAndDiscoverSuccess(status: Int) {
        launch {
            lastState = LastState.CONNECT_CONNECTED
            isActiveDisconnect = false
            BleManager.multipleBluetoothController.removeConnectingBle(this@BleBluetooth)
            BleManager.multipleBluetoothController.addConnectedBleBluetooth(this@BleBluetooth)
            bleGattCallback?.onConnectSuccess(bleDevice, bluetoothGatt, status)
        }
    }

    @Synchronized
    fun disconnect() {
        isActiveDisconnect = true
        disconnectGatt()
    }

    @Synchronized
    private fun disconnectGatt() {
        bluetoothGatt?.disconnect()
    }

    @Synchronized
    private fun refreshDeviceCache() {
        try {
            val refresh = BluetoothGatt::class.java.getMethod("refresh")
            if (bluetoothGatt != null) {
                val success = refresh.invoke(bluetoothGatt) as Boolean
                BleLog.i("refreshDeviceCache, is success:  $success")
            }
        } catch (e: Exception) {
            BleLog.i("exception occur while refreshing device: " + e.message)
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun closeBluetoothGatt() {
        bluetoothGatt?.close()
    }

    @Synchronized
    fun destroy() {
        lastState = LastState.CONNECT_IDLE
        disconnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        bleGattCallback = null
        removeRssiOperator()
        removeMtuOperator()
        clearCharacterOperator()
        cancel()
    }

    @Synchronized
    fun clearCharacterOperator() {
        for (entry: Map.Entry<String?, BleOperator> in bleNotifyOperatorMap) {
            entry.value.destroy()
        }
        bleNotifyOperatorMap.clear()

        for (entry: Map.Entry<String?, BleOperator> in bleIndicateOperatorMap) {
            entry.value.destroy()
        }
        bleIndicateOperatorMap.clear()

        for (entry: Map.Entry<String?, BleOperator> in bleWriteOperatorMap) {
            entry.value.destroy()
        }
        bleWriteOperatorMap.clear()

        for (entry: Map.Entry<String?, BleOperator> in bleReadOperatorMap) {
            entry.value.destroy()
        }
        bleReadOperatorMap.clear()
    }

    @Synchronized
    fun addNotifyOperator(uuid: String, operator: BleOperator) {
        if (bleNotifyOperatorMap[uuid] != operator) {
            bleNotifyOperatorMap[uuid]?.destroy()
            bleNotifyOperatorMap[uuid] = operator
        }
    }

    @Synchronized
    fun removeNotifyOperator(uuid: String) {
        if (bleNotifyOperatorMap.containsKey(uuid)) {
            bleNotifyOperatorMap[uuid]?.destroy()
            bleNotifyOperatorMap.remove(uuid)
        }
    }

    @Synchronized
    fun addIndicateOperator(uuid: String, operator: BleOperator) {
        if (bleIndicateOperatorMap[uuid] != operator) {
            bleIndicateOperatorMap[uuid]?.destroy()
            bleIndicateOperatorMap[uuid] = operator
        }
    }

    @Synchronized
    fun removeIndicateOperator(uuid: String) {
        if (bleIndicateOperatorMap[uuid] != null) {
            bleIndicateOperatorMap[uuid]?.destroy()
            bleIndicateOperatorMap.remove(uuid)
        }
    }

    @Synchronized
    fun addWriteOperator(uuid: String, operator: BleOperator) {
        if (bleWriteOperatorMap[uuid] != operator) {
            bleWriteOperatorMap[uuid]?.destroy()
            bleWriteOperatorMap[uuid] = operator
        }
    }

    @Synchronized
    fun removeWriteOperator(uuid: String?) {
        if (bleWriteOperatorMap.containsKey(uuid)) {
            bleWriteOperatorMap[uuid]?.destroy()
            bleWriteOperatorMap.remove(uuid)
        }
    }

    @Synchronized
    fun addReadOperator(uuid: String, operator: BleOperator) {
        if (bleReadOperatorMap[uuid] != operator) {
            bleReadOperatorMap[uuid]?.destroy()
            bleReadOperatorMap[uuid] = operator
        }
    }

    @Synchronized
    fun removeReadOperator(uuid: String?) {
        if (bleReadOperatorMap.containsKey(uuid)) {
            bleReadOperatorMap[uuid]?.destroy()
            bleReadOperatorMap.remove(uuid)
        }
    }

    @Synchronized
    fun setRssiOperator(operator: BleOperator) {
        if (bleRssiOperator != operator) {
            bleRssiOperator?.destroy()
            bleRssiOperator = operator
        }
    }

    @Synchronized
    fun removeRssiOperator() {
        bleRssiOperator?.destroy()
        bleRssiOperator = null
    }

    @Synchronized
    fun setMtuOperator(operator: BleOperator) {
        if (bleMtuOperator != operator) {
            bleMtuOperator?.destroy()
            bleMtuOperator = operator
        }
    }

    @Synchronized
    fun removeMtuOperator() {
        bleMtuOperator?.destroy()
        bleMtuOperator = null
    }

    enum class LastState {
        CONNECT_IDLE, CONNECT_CONNECTING, CONNECT_CONNECTED, CONNECT_FAILURE, CONNECT_DISCONNECT
    }


    private val coreGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            BleLog.i(
                """
                     BluetoothGattCallback：onConnectionStateChange 
                     status: $status
                     newState: $newState
                     currentThread: ${Thread.currentThread().id} isMain: ${Looper.myLooper() == Looper.getMainLooper()}
                     """.trimIndent()
            )
            bluetoothGatt = gatt
            if (connectTimeOutTask.hasTask()) {
                connectTimeOutTask.success()
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                launch(Dispatchers.IO) {
                    delay(50)
                    discoverService()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (lastState == LastState.CONNECT_CONNECTING) {
                    disconnectGatt()
                    refreshDeviceCache()
                    closeBluetoothGatt()
                    if (currentConnectRetryCount < BleManager.reConnectCount) {
                        BleLog.e(
                            "Connect fail, try reconnect " + BleManager.reConnectInterval + " millisecond later"
                        )
                        currentConnectRetryCount++
                        launch {
                            delay(BleManager.reConnectInterval)
                            connect(BleManager.context, false, currentConnectRetryCount)
                        }
                    } else {
                        launch {
                            connectedFail(BleException.ConnectException(bluetoothGatt, status))
                        }
                    }
                } else if (lastState == LastState.CONNECT_CONNECTED) {
                    lastState = LastState.CONNECT_DISCONNECT
                    launch {
                        bleGattCallback?.onDisConnected(
                            isActiveDisconnect,
                            bleDevice,
                            bluetoothGatt,
                            status
                        )
                        BleManager.multipleBluetoothController.removeConnectedBleBluetooth(this@BleBluetooth)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            BleLog.i(
                """
                       BluetoothGattCallback：onServicesDiscovered 
                       status: $status
                       currentThread: ${Thread.currentThread().id}
                       """.trimIndent()
            )
            bluetoothGatt = gatt
            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectAndDiscoverSuccess(status)
            } else {
                discoverFail()
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                for ((uuid, operator) in bleNotifyOperatorMap) {
                    if (operator.operateCallback is BleNotifyCallback) {
                        if (characteristic.uuid.toString().equals(uuid, true)) {
                            launch {
                                (operator.operateCallback as? BleNotifyCallback)?.onCharacteristicChanged(
                                    value
                                )
                            }
                        }
                    }
                }

                for ((uuid, operator) in bleIndicateOperatorMap) {
                    if (operator.operateCallback is BleIndicateCallback) {
                        if (characteristic.uuid.toString().equals(uuid, true)) {
                            launch {
                                (operator.operateCallback as? BleIndicateCallback)?.onCharacteristicChanged(
                                    value
                                )
                            }
                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                for ((uuid, operator) in bleNotifyOperatorMap) {
                    if (operator.operateCallback is BleNotifyCallback) {
                        if (characteristic?.uuid.toString().equals(uuid, true)) {
                            launch {
                                (operator.operateCallback as? BleNotifyCallback)?.onCharacteristicChanged(
                                    characteristic?.value
                                )
                            }
                        }
                    }
                }
                for ((uuid, operator) in bleIndicateOperatorMap) {
                    if (operator.operateCallback is BleIndicateCallback) {
                        if (characteristic?.uuid.toString().equals(uuid, true)) {
                            launch {
                                (operator.operateCallback as? BleIndicateCallback)?.onCharacteristicChanged(
                                    characteristic?.value
                                )
                            }
                        }
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            for ((uuid, operator) in bleNotifyOperatorMap) {
                if (operator.operateCallback is BleNotifyCallback) {
                    if (descriptor?.characteristic?.uuid.toString().equals(uuid, true)) {
                        operator.removeTimeOut()
                        launch {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                (operator.operateCallback as? BleNotifyCallback)?.onNotifySuccess()
                            } else {
                                (operator.operateCallback as? BleNotifyCallback)?.onNotifyFailure(
                                    BleException.GattException(gatt, status)
                                )
                            }
                        }
                    }
                }
            }

            for ((uuid, operator) in bleIndicateOperatorMap) {
                if (operator.operateCallback is BleIndicateCallback) {
                    if (descriptor?.characteristic?.uuid.toString().equals(uuid, true)) {
                        operator.removeTimeOut()
                        launch {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                (operator.operateCallback as? BleIndicateCallback)?.onIndicateSuccess()
                            } else {
                                (operator.operateCallback as? BleIndicateCallback)?.onIndicateFailure(
                                    BleException.GattException(gatt, status)
                                )
                            }
                        }
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        /**
         * android13 Build.VERSION_CODES.TIRAMISU 这个方法characteristic?.value 一直为null
         * @see BleOperator.writeCharacteristic
         */
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            for ((uuid, operator) in bleWriteOperatorMap) {
                if (operator.operateCallback is BleWriteCallback) {
                    if (characteristic?.uuid.toString().equals(uuid, true)) {
                        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            operator.data
                        } else {
                            characteristic?.value
                        }
                        operator.removeTimeOut()
                        launch {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                (operator.operateCallback as? BleWriteCallback)?.onWriteSuccess(
                                    justWrite = data
                                )
                            } else {
                                (operator.operateCallback as? BleWriteCallback)?.onWriteFailure(
                                    BleException.GattException(gatt, status),
                                    justWrite = data
                                )
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                for ((uuid, operator) in bleReadOperatorMap) {
                    if (operator.operateCallback is BleReadCallback) {
                        if (characteristic.uuid.toString().equals(uuid, true)) {
                            operator.removeTimeOut()
                            launch {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    (operator.operateCallback as? BleReadCallback)?.onReadSuccess(
                                        value
                                    )
                                } else {
                                    (operator.operateCallback as? BleReadCallback)?.onReadFailure(
                                        BleException.GattException(gatt, status)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                for ((uuid, operator) in bleReadOperatorMap) {
                    if (operator.operateCallback is BleReadCallback) {
                        if (characteristic?.uuid.toString().equals(uuid, true)) {
                            operator.removeTimeOut()
                            launch {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    (operator.operateCallback as? BleReadCallback)?.onReadSuccess(
                                        characteristic?.value
                                    )
                                } else {
                                    (operator.operateCallback as? BleReadCallback)?.onReadFailure(
                                        BleException.GattException(gatt, status)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            launch {
                bleRssiOperator?.let {
                    it.removeTimeOut()
                    if (it.operateCallback is BleRssiCallback) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            (it.operateCallback as? BleRssiCallback)?.onRssiSuccess(rssi)
                        } else {
                            (it.operateCallback as? BleRssiCallback)?.onRssiFailure(
                                BleException.GattException(
                                    gatt,
                                    status
                                )
                            )
                        }
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            bleMtuOperator?.let {
                it.removeTimeOut()
                if (it.operateCallback is BleMtuChangedCallback) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        (it.operateCallback as? BleMtuChangedCallback)?.onMtuChanged(mtu)
                    } else {
                        (it.operateCallback as? BleMtuChangedCallback)?.onSetMTUFailure(
                            BleException.GattException(
                                gatt,
                                status
                            )
                        )
                    }
                }
            }
        }
    }

    private fun discoverService() {
        if (bluetoothGatt != null) {
            val discoverServiceResult = bluetoothGatt!!.discoverServices()
            if (!discoverServiceResult) {
                discoverFail()
            }
        } else {
            discoverFail()
        }
    }

}