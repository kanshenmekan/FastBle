package com.huyuhui.fastble.bluetooth

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
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.callback.BleGattCallback
import com.huyuhui.fastble.common.BleConnectStrategy
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.exception.BleMainScope
import com.huyuhui.fastble.queue.operate.BleOperatorQueue
import com.huyuhui.fastble.queue.operate.SequenceBleOperator
import com.huyuhui.fastble.utils.BleLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
internal class BleBluetooth(val bleDevice: BleDevice) :
    CoroutineScope by BleMainScope({ _, throwable ->
        BleLog.e("BleDevice $bleDevice: a coroutine error has occurred ${throwable.message}")
        BleManager.cancelOrDisconnect(bleDevice)
    }) {

    companion object {
        const val DEFAULT_QUEUE_IDENTIFIER = "com.hyh.ble.bluetooth.BleBluetooth"
    }

    private val connectTimeOutTask by lazy {
        TimeoutTask(
            bleConnectStrategy.connectOverTime, object : TimeoutTask.OnResultCallBack {
                override fun onError(task: TimeoutTask, e: Throwable?, isActive: Boolean) {
                    super.onError(task, e, isActive)
                    connectedFail(BleException.TimeoutException())
                }
            }
        )
    }
    var bleGattCallback: BleGattCallback? = null
        @Synchronized
        set

    private val bleNotifyOperatorMap: ConcurrentHashMap<String, BleNotifyOperator> =
        ConcurrentHashMap()
    private val bleIndicateOperatorMap: ConcurrentHashMap<String, BleIndicateOperator> =
        ConcurrentHashMap()
    private val bleWriteOperatorMap: ConcurrentHashMap<String, BleWriteOperator> =
        ConcurrentHashMap()
    private val bleReadOperatorMap: ConcurrentHashMap<String, BleReadOperator> = ConcurrentHashMap()
    private val bleOperatorQueueMap: ConcurrentHashMap<String, BleOperatorQueue> =
        ConcurrentHashMap()
    private var bleRssiOperator: BleReadRssiOperator? = null
    private var bleMtuOperator: BleMtuOperator? = null
    private var lastState: LastState? = null
    private var isActiveDisconnect = false
    var bluetoothGatt: BluetoothGatt? = null
        private set
    private var currentConnectRetryCount = 0

    private lateinit var bleConnectStrategy: BleConnectStrategy
    val deviceKey
        get() = bleDevice.key

    @Synchronized
    fun connect(
        context: Context,
        autoConnect: Boolean,
        bleConnectStrategy: BleConnectStrategy,
        callback: BleGattCallback?,
    ): BluetoothGatt? {
        if (!isActive) throw Exception("this $this is destroyed,do not connect")
        bleGattCallback = callback
        this.bleConnectStrategy = bleConnectStrategy
        currentConnectRetryCount = 0
        if (BleManager.multipleBluetoothController.isConnectedDevice(bleDevice)) {
            bleGattCallback?.onConnectCancel(bleDevice, true)
            return BleManager.getBluetoothGatt(bleDevice)
        }
        return connect(context, autoConnect, currentConnectRetryCount)
    }

    @Synchronized
    private fun connect(
        context: Context,
        autoConnect: Boolean,
        connectRetryCount: Int,
    ): BluetoothGatt? {
        if (!isActive) return bluetoothGatt
        discoverServiceJob?.takeIf { it.isActive }?.cancel()
        BleLog.i(
            """
                connect device: ${bleDevice.name}
                mac: ${bleDevice.mac}
                autoConnect: $autoConnect
                currentThread: ${Thread.currentThread().id}
                connectCount:${connectRetryCount + 1}
                isMain: ${Looper.myLooper() == Looper.getMainLooper()}
                """.trimIndent()
        )
        if (bleConnectStrategy.connectOverTime > 0) {
            connectTimeOutTask.start(this)
        }
        lastState = LastState.CONNECT_CONNECTING
        bluetoothGatt =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bleDevice.device.connectGatt(
                    context, autoConnect, coreGattCallback,
                    if (bleConnectStrategy.transport == 0) BluetoothDevice.TRANSPORT_AUTO else bleConnectStrategy.transport,
                    if (bleConnectStrategy.phy == 1) BluetoothDevice.PHY_LE_1M_MASK else bleConnectStrategy.phy
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bleDevice.device.connectGatt(
                    context, autoConnect, coreGattCallback,
                    if (bleConnectStrategy.transport == 0) BluetoothDevice.TRANSPORT_AUTO else bleConnectStrategy.transport
                )
            } else {
                bleDevice.device.connectGatt(context, autoConnect, coreGattCallback)
            }
        if (bluetoothGatt != null) {
            if (connectRetryCount == 0) {
                bleGattCallback?.onStartConnect(bleDevice)
                BleLog.i("Start connecting device $bleDevice,$bleConnectStrategy")
            }
        } else {
            connectTimeOutTask.cancel()
            connectedFail(BleException.OtherException(BleException.GATT_NULL, "GATT is null!"))
        }
        return bluetoothGatt
    }

    private fun connectedFail(exception: BleException) {
        launch(Dispatchers.Main.immediate) {
            lastState = LastState.CONNECT_FAILURE
            BleManager.multipleBluetoothController.removeConnectingBle(this@BleBluetooth)
            bleGattCallback?.onConnectFail(
                bleDevice,
                exception
            )
            BleLog.i("Connection failure,$exception")
            destroy()
        }
    }

    fun buildNotifyOperator(
        uuidService: String,
        uuidCharacteristic: String,
        timeout: Long
    ): BleNotifyOperator {
        return BleNotifyOperator(this, timeout).withUUIDString(uuidService, uuidCharacteristic)
    }

    fun buildIndicateOperator(
        uuidService: String,
        uuidCharacteristic: String,
        timeout: Long
    ): BleIndicateOperator {
        return BleIndicateOperator(this, timeout).withUUIDString(uuidService, uuidCharacteristic)
    }

    fun buildWriteOperator(
        uuidService: String,
        uuidCharacteristic: String,
        timeout: Long
    ): BleWriteOperator {
        return BleWriteOperator(this, timeout).withUUIDString(uuidService, uuidCharacteristic)
    }

    fun buildReadOperator(
        uuidService: String,
        uuidCharacteristic: String,
        timeout: Long
    ): BleReadOperator {
        return BleReadOperator(this, timeout).withUUIDString(uuidService, uuidCharacteristic)
    }

    fun buildRssiOperator(timeout: Long): BleReadRssiOperator {
        return BleReadRssiOperator(this, timeout)
    }

    fun buildMtuOperator(timeout: Long): BleMtuOperator {
        return BleMtuOperator(this, timeout)
    }

    private fun createOperateQueue(identifier: String = DEFAULT_QUEUE_IDENTIFIER): Boolean {
        val newQueue = BleOperatorQueue(this)
        val existingQueue = bleOperatorQueueMap.putIfAbsent(identifier, newQueue)
        return if (existingQueue == null) {
            newQueue.startProcessingTasks() // 新队列启动
            true
        } else {
            existingQueue.startProcessingTasks() // 已有队列启动
            false
        }
    }


    fun removeOperateQueue(identifier: String = DEFAULT_QUEUE_IDENTIFIER) {
        bleOperatorQueueMap[identifier]?.destroy()
        bleOperatorQueueMap.remove(identifier)
    }


    fun removeOperatorFromQueue(
        identifier: String = DEFAULT_QUEUE_IDENTIFIER,
        sequenceBleOperator: SequenceBleOperator,
    ): Boolean {
        return bleOperatorQueueMap[identifier]?.remove(sequenceBleOperator) != false
    }


    fun addOperatorToQueue(
        identifier: String = DEFAULT_QUEUE_IDENTIFIER,
        sequenceBleOperator: SequenceBleOperator,
    ): Boolean {
        createOperateQueue(identifier)
        return bleOperatorQueueMap[identifier]?.offer(sequenceBleOperator) == true
    }


    fun clearQueue(identifier: String = DEFAULT_QUEUE_IDENTIFIER) {
        bleOperatorQueueMap[identifier]?.clear()
    }


    fun pauseQueue(identifier: String = DEFAULT_QUEUE_IDENTIFIER) {
        bleOperatorQueueMap[identifier]?.pause()
    }


    fun resume(identifier: String = DEFAULT_QUEUE_IDENTIFIER) {
        bleOperatorQueueMap[identifier]?.resume()
    }

    private fun discoverFail(exception: BleException.DiscoverException = BleException.DiscoverException()) {
        discoverServiceJob?.takeIf { it.isActive }?.cancel()
        connectedFail(exception)
    }

    private fun connectAndDiscoverSuccess(status: Int) {
        discoverServiceJob?.takeIf { it.isActive }?.cancel()
        launch(Dispatchers.Main.immediate) {
            lastState = LastState.CONNECT_CONNECTED
            isActiveDisconnect = false
            BleManager.multipleBluetoothController.removeConnectingBle(this@BleBluetooth)
            BleManager.multipleBluetoothController.addConnectedBleBluetooth(this@BleBluetooth)
            bleGattCallback?.onConnectSuccess(bleDevice, bluetoothGatt, status)
            BleLog.i("Connect success,$bleDevice")
        }
    }

    @Synchronized
    fun disconnect() {
        isActiveDisconnect = true
        connectTimeOutTask.cancel() // 取消连接超时任务
        discoverServiceJob?.cancel() // 取消发现服务超时
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
        bluetoothGatt = null
    }

    @Synchronized
    fun destroy() {
        lastState = LastState.CONNECT_IDLE
        disconnect()
        refreshDeviceCache()
        closeBluetoothGatt()
        bleGattCallback = null
        removeRssiOperator()
        removeMtuOperator()
        clearCharacterOperator()
        clearOperatorQueue()
        connectTimeOutTask.onTimeoutResultCallBack = null
        cancel()
    }

    @Synchronized
    fun clearOperatorQueue() {
        for (entry: Map.Entry<String?, BleOperatorQueue> in bleOperatorQueueMap) {
            entry.value.destroy()
        }
        bleOperatorQueueMap.clear()
    }


    @Synchronized
    fun clearCharacterOperator() {
        bleNotifyOperatorMap.values.forEach {
            it.destroy()
        }
        bleNotifyOperatorMap.clear()

        bleIndicateOperatorMap.values.forEach {
            it.destroy()
        }
        bleIndicateOperatorMap.clear()

        bleWriteOperatorMap.values.forEach {
            it.destroy()
        }
        bleWriteOperatorMap.clear()

        bleReadOperatorMap.values.forEach {
            it.destroy()
        }
        bleReadOperatorMap.clear()
    }

    fun addNotifyOperator(uuid: String, operator: BleNotifyOperator) {
        val oldOperator = bleNotifyOperatorMap.put(uuid, operator)
        // 仅当旧值存在且与新值不同时，销毁旧值
        if (oldOperator != null && oldOperator != operator) {
            oldOperator.destroy()
        }
    }

    fun removeNotifyOperator(uuid: String) {
        bleNotifyOperatorMap.remove(uuid)?.destroy()
    }

    fun addIndicateOperator(uuid: String, operator: BleIndicateOperator) {
        val oldOperator = bleIndicateOperatorMap.put(uuid, operator)
        // 仅当旧值存在且与新值不同时，销毁旧值
        if (oldOperator != null && oldOperator != operator) {
            oldOperator.destroy()
        }
    }


    fun removeIndicateOperator(uuid: String) {
        bleIndicateOperatorMap.remove(uuid)?.destroy()
    }


    fun addWriteOperator(uuid: String, operator: BleWriteOperator) {
        val oldOperator = bleWriteOperatorMap.put(uuid, operator)
        // 仅当旧值存在且与新值不同时，销毁旧值
        if (oldOperator != null && oldOperator != operator) {
            oldOperator.destroy()
        }
    }


    fun removeWriteOperator(uuid: String?) {
        bleWriteOperatorMap.remove(uuid)?.destroy()
    }

    fun addReadOperator(uuid: String, operator: BleReadOperator) {
        val oldOperator = bleReadOperatorMap.put(uuid, operator)
        // 仅当旧值存在且与新值不同时，销毁旧值
        if (oldOperator != null && oldOperator != operator) {
            oldOperator.destroy()
        }
    }

    fun removeReadOperator(uuid: String?) {
        bleReadOperatorMap.remove(uuid)?.destroy()
    }

    fun setRssiOperator(operator: BleReadRssiOperator) {
        if (bleRssiOperator != operator) {
            bleRssiOperator?.destroy()
            bleRssiOperator = operator
        }
    }

    fun removeRssiOperator() {
        bleRssiOperator?.destroy()
        bleRssiOperator = null
    }

    fun setMtuOperator(operator: BleMtuOperator) {
        if (bleMtuOperator != operator) {
            bleMtuOperator?.destroy()
            bleMtuOperator = operator
        }
    }

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
                    if (currentConnectRetryCount < bleConnectStrategy.reConnectCount) {
                        disconnectGatt()
                        refreshDeviceCache()
                        closeBluetoothGatt()
                        currentConnectRetryCount++
                        launch {
                            BleLog.e(
                                "Connect fail, try reconnect " + bleConnectStrategy.reConnectInterval + " millisecond later"
                            )
                            delay(bleConnectStrategy.reConnectInterval)
                            connect(BleManager.context!!, false, currentConnectRetryCount)
                        }
                    } else {
                        connectedFail(BleException.ConnectException(bluetoothGatt, status))
                    }
                } else if (lastState == LastState.CONNECT_CONNECTED) {
                    lastState = LastState.CONNECT_DISCONNECT
                    BleManager.multipleBluetoothController.removeConnectedBleBluetooth(this@BleBluetooth)
                    launch {
                        bleGattCallback?.onDisConnected(
                            isActiveDisconnect,
                            bleDevice,
                            bluetoothGatt,
                            status
                        )
                        BleLog.i("disconnect,$bleDevice,isActiveDisconnect = $isActiveDisconnect")
                        destroy()
                    }
                }
            }
            bleGattCallback?.onConnectionStateChange(gatt, status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            BleLog.i(
                """
                       BluetoothGattCallback：onServicesDiscovered 
                       status: $status
                       currentThread: ${Thread.currentThread().id}
                       isMain: ${Looper.myLooper() == Looper.getMainLooper()}
                       """.trimIndent()
            )
            bluetoothGatt = gatt
            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectAndDiscoverSuccess(status)
            } else {
                discoverFail()
            }
            launch {
                bleGattCallback?.onServicesDiscovered(bleDevice, gatt, status)
            }
            bleGattCallback?.onServicesDiscovered(gatt, status)
        }

        //        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                for ((uuid, operator) in bleNotifyOperatorMap) {
                    if (characteristic.uuid.toString().equals(uuid, true)) {
                        operator.launch {
                            operator.bleNotifyCallback?.onCharacteristicChanged(
                                bleDevice, characteristic,
                                value
                            )
                        }
                        break
                    }
                }

                for ((uuid, operator) in bleIndicateOperatorMap) {
                    if (characteristic.uuid.toString()
                            .equals(uuid, true)
                    ) {
                        operator.launch {
                            operator.bleIndicateCallback?.onCharacteristicChanged(
                                bleDevice, characteristic,
                                value
                            )
                        }
                        break
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bleGattCallback?.onCharacteristicChanged(gatt, characteristic, value)
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                for ((uuid, operator) in bleNotifyOperatorMap) {
                    if (characteristic?.uuid.toString().equals(uuid, true)) {
                        operator.launch {
                            operator.bleNotifyCallback?.onCharacteristicChanged(
                                bleDevice, characteristic!!,
                                characteristic.value
                            )
                        }
                        break
                    }
                }

                for ((uuid, operator) in bleIndicateOperatorMap) {
                    if (characteristic?.uuid.toString().equals(uuid, true)) {
                        operator.launch {
                            operator.bleIndicateCallback?.onCharacteristicChanged(
                                bleDevice, characteristic!!,
                                characteristic.value
                            )
                        }
                        break
                    }
                }
            }
            bleGattCallback?.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int,
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            @Suppress("DEPRECATION")
            val data = descriptor?.value
            for ((uuid, operator) in bleNotifyOperatorMap) {
                if (descriptor?.characteristic?.uuid.toString().equals(uuid, true)) {
                    if (data.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        operator.removeTimeOut()
                        operator.launch {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                operator.bleNotifyCallback?.onNotifySuccess(
                                    bleDevice,
                                    descriptor!!.characteristic
                                )
                            } else {
                                operator.bleNotifyCallback?.onNotifyFailure(
                                    bleDevice, descriptor!!.characteristic,
                                    BleException.GattException(gatt, status)
                                )
                            }
                        }
                    } else if (data.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            operator.launch {
                                bleNotifyOperatorMap[descriptor!!.characteristic.uuid.toString()]?.bleNotifyCallback?.onNotifyCancel(
                                    bleDevice,
                                    descriptor.characteristic
                                )
                                removeNotifyOperator(descriptor.characteristic.uuid.toString())
                            }
                        }
                    }
                    break
                }
            }

            for ((uuid, operator) in bleIndicateOperatorMap) {
                if (descriptor?.characteristic?.uuid.toString().equals(uuid, true)) {
                    if (data.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                        operator.removeTimeOut()
                        operator.launch {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                operator.bleIndicateCallback?.onIndicateSuccess(
                                    bleDevice,
                                    descriptor!!.characteristic
                                )
                                BleLog.i("onIndicateSuccess,characteristic = ${descriptor!!.characteristic.uuid}")
                            } else {
                                operator.bleIndicateCallback?.onIndicateFailure(
                                    bleDevice, descriptor!!.characteristic,
                                    BleException.GattException(gatt, status)
                                )
                            }
                        }
                    } else if (data.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            operator.launch {
                                bleIndicateOperatorMap[descriptor!!.characteristic.uuid.toString()]?.bleIndicateCallback?.onIndicateCancel(
                                    bleDevice,
                                    descriptor.characteristic
                                )
                                removeIndicateOperator(descriptor.characteristic.uuid.toString())
                            }
                        }
                    }
                    break
                }
            }
            bleGattCallback?.onDescriptorWrite(gatt, descriptor, status)
        }

        /**
         * android13 Build.VERSION_CODES.TIRAMISU 不用Characteristic!!.setValue(data)这个方法，characteristic?.value 一直为null
         * @see BleWriteOperator.writeCharacteristic
         */
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int,
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            for ((uuid, operator) in bleWriteOperatorMap) {
                if (characteristic?.uuid.toString().equals(uuid, true)) {
//                    val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                        operator.data
//                    } else {
//                        characteristic?.value
//                    }
                    val data = operator.data
                    operator.removeTimeOut()
                    //这里改用BleBluetooth的协程作用域，当发送很快，触发这个回调也很快，上一个operator可能被当前这个取消，导致回调不能被执行
                    //operator销毁之后callback也被置为null了，这里先记录一下
                    val callback = operator.bleWriteCallback
                    launch(Dispatchers.Main.immediate) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            callback?.onWriteSuccess(
                                bleDevice = bleDevice, characteristic = characteristic!!,
                                justWrite = data!!
                            )
                        } else {
                            callback?.onWriteFailure(
                                bleDevice, characteristic,
                                BleException.GattException(gatt, status),
                                justWrite = data
                            )
                        }
                    }
                    break
                }
            }
            bleGattCallback?.onCharacteristicWrite(gatt, characteristic, status)
        }

        //        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                for ((uuid, operator) in bleReadOperatorMap) {
                    if (characteristic.uuid.toString().equals(uuid, true)) {
                        operator.removeTimeOut()
                        operator.launch {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                operator.bleReadCallback?.onReadSuccess(
                                    bleDevice, characteristic,
                                    value
                                )
                            } else {
                                operator.bleReadCallback?.onReadFailure(
                                    bleDevice, characteristic,
                                    BleException.GattException(gatt, status)
                                )
                            }
                        }
                    }
                }
                bleGattCallback?.onCharacteristicRead(gatt, characteristic, value, status)
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int,
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                for ((uuid, operator) in bleReadOperatorMap) {
                    if (characteristic?.uuid.toString().equals(uuid, true)) {
                        operator.removeTimeOut()
                        operator.launch {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                operator.bleReadCallback?.onReadSuccess(
                                    bleDevice, characteristic!!,
                                    characteristic.value
                                )
                            } else {
                                operator.bleReadCallback?.onReadFailure(
                                    bleDevice, characteristic,
                                    BleException.GattException(gatt, status)
                                )
                            }
                        }
                    }
                }
            }
            bleGattCallback?.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            launch {
                bleRssiOperator?.let {
                    it.removeTimeOut()
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        it.bleRssiCallback?.onRssiSuccess(bleDevice, rssi)
                    } else {
                        it.bleRssiCallback?.onRssiFailure(
                            bleDevice,
                            BleException.GattException(
                                gatt,
                                status
                            )
                        )
                    }
                }
            }
            bleGattCallback?.onReadRemoteRssi(gatt, rssi, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            launch {
                bleMtuOperator?.let {
                    it.removeTimeOut()
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        it.bleMtuChangedCallback?.onMtuChanged(
                            bleDevice,
                            mtu
                        )
                    } else {
                        it.bleMtuChangedCallback?.onSetMTUFailure(
                            bleDevice,
                            BleException.GattException(
                                gatt,
                                status
                            )
                        )
                    }
                }

            }
            bleGattCallback?.onMtuChanged(gatt, mtu, status)
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launch {
                    bleGattCallback?.onPhyUpdate(bleDevice, gatt, txPhy, rxPhy, status)
                }
                bleGattCallback?.onPhyUpdate(gatt, txPhy, rxPhy, status)
            }
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launch {
                    bleGattCallback?.onPhyRead(bleDevice, gatt, txPhy, rxPhy, status)
                }
                bleGattCallback?.onPhyRead(gatt, txPhy, rxPhy, status)
            }
        }

    }
    private var discoverServiceJob: Job? = null
    private fun discoverService() {
        discoverServiceJob?.takeIf { it.isActive }?.cancel()
        if (bluetoothGatt != null) {
            discoverServiceJob = launch {
                delay(bleConnectStrategy.discoverServiceTimeout)
                BleLog.e("discoverService timeout ${bleConnectStrategy.discoverServiceTimeout}")
                discoverFail(
                    BleException.DiscoverException(
                        BleException.ERROR_CODE_TIMEOUT,
                        "discoverService timeout ${bleConnectStrategy.discoverServiceTimeout}"
                    )
                )
            }
            val discoverServiceResult = bluetoothGatt!!.discoverServices()

            if (!discoverServiceResult) {
                discoverFail()
            }

        } else {
            discoverFail()
        }
    }

}