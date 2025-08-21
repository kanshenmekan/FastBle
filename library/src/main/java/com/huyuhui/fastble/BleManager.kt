package com.huyuhui.fastble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.huyuhui.fastble.bluetooth.BleBluetooth
import com.huyuhui.fastble.bluetooth.BleOperator
import com.huyuhui.fastble.bluetooth.MultipleBluetoothController
import com.huyuhui.fastble.bluetooth.SplitWriter
import com.huyuhui.fastble.callback.BleGattCallback
import com.huyuhui.fastble.callback.BleIndicateCallback
import com.huyuhui.fastble.callback.BleMtuChangedCallback
import com.huyuhui.fastble.callback.BleNotifyCallback
import com.huyuhui.fastble.callback.BleReadCallback
import com.huyuhui.fastble.callback.BleRssiCallback
import com.huyuhui.fastble.callback.BleScanCallback
import com.huyuhui.fastble.callback.BleWriteCallback
import com.huyuhui.fastble.common.BleConnectStrategy
import com.huyuhui.fastble.common.BleFactory
import com.huyuhui.fastble.common.BluetoothChangedObserver
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.data.BleScanState
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.queue.operate.SequenceBleOperator
import com.huyuhui.fastble.scan.BleScanRuleConfig
import com.huyuhui.fastble.scan.BleScanner
import com.huyuhui.fastble.utils.BleLog

@Suppress("unused")
object BleManager {
    const val DEFAULT_SCAN_TIME: Long = 10000
    private const val DEFAULT_MAX_MULTIPLE_DEVICE = 7
    private const val DEFAULT_OPERATE_TIME: Long = 5000
    private const val DEFAULT_MTU = 23
    private const val DEFAULT_MAX_MTU = 512
    private const val DEFAULT_WRITE_DATA_SPLIT_COUNT = 20

    private var bleObserver: BluetoothChangedObserver? = null

    /**
     * the maximum number of connections
     */
    var maxConnectCount = DEFAULT_MAX_MULTIPLE_DEVICE
        set(value) {
            field = if (value > DEFAULT_MAX_MULTIPLE_DEVICE) {
                DEFAULT_MAX_MULTIPLE_DEVICE
            } else {
                value
            }
        }

    /**
     * operate timeout
     */
    var operateTimeout = DEFAULT_OPERATE_TIME

    /**
     * operate split Write Num
     */
    var splitWriteNum = DEFAULT_WRITE_DATA_SPLIT_COUNT
        set(value) {
            if (value > 0) {
                field = value
            }
        }


    var context: Application? = null
        private set
    var bleScanRuleConfig: BleScanRuleConfig = BleScanRuleConfig.Builder().build()
    var bleConnectStrategy: BleConnectStrategy = BleConnectStrategy.Builder().build()
    var bleFactory: BleFactory? = null
    val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter


    //多设备连接管理
    internal val multipleBluetoothController: MultipleBluetoothController =
        MultipleBluetoothController()

    @SuppressLint("PrivateApi")
    var bluetoothManager: BluetoothManager? = null
        private set

    fun init(app: Application) {
        context = app
        if (isSupportBle(app)) {
            bluetoothManager =
                context!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    fun scan(bleScanCallback: BleScanCallback?, timeout: Long = bleScanRuleConfig.mScanTimeOut) {
        if (context == null) {
            BleLog.e("BleManager may not be initialized")
            return
        }
        if (!isBleEnable(context!!)) {
            bleScanCallback?.onScanStarted(false)
            return
        }
        BleScanner.bleScanCallback = bleScanCallback
        BleScanner.startLeScan(timeout)
    }

    /**
     * Cancel scan
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    fun cancelScan() {
        BleScanner.stopLeScan()
    }

    fun enableLog(enable: Boolean): BleManager {
        BleLog.isPrint = enable
        return this
    }

    /**
     * connect a known device
     *
     * @param bleDevice
     * @param bleGattCallback
     * @param strategy CONNECT_BACKPRESSURE_DROP,CONNECT_BACKPRESSURE_LAST
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun connect(
        bleDevice: BleDevice,
        bleGattCallback: BleGattCallback?,
        strategy: BleConnectStrategy = bleConnectStrategy,
    ): BluetoothGatt? {
        if (context == null) {
            BleLog.e("BleManager may not be initialized")
            return null
        }
        if (!isSupportBle(context)) {
            bleGattCallback?.onConnectFail(
                bleDevice,
                BleException.OtherException(BleException.NOT_SUPPORT_BLE, "Bluetooth not support!")
            )
            return null
        }
        if (!isBleEnable(context!!)) {
            BleLog.e("Bluetooth not enable!")
            bleGattCallback?.onConnectFail(
                bleDevice,
                BleException.OtherException(
                    BleException.BLUETOOTH_NOT_ENABLED,
                    "Bluetooth not enable!"
                )
            )
            return null
        }
        if (Looper.myLooper() == null || Looper.myLooper() != Looper.getMainLooper()) {
            BleLog.w("Be careful: currentThread is not MainThread!")
        }

        if (multipleBluetoothController.isConnectedDevice(bleDevice)) {
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)?.bleGattCallback =
                bleGattCallback
            bleGattCallback?.onConnectCancel(bleDevice, true)
            return getBluetoothGatt(bleDevice)
        }
        if (strategy.connectBackpressureStrategy == BleConnectStrategy.CONNECT_BACKPRESSURE_DROP) {
            return if (multipleBluetoothController.isConnecting(bleDevice)) {
                val bleBluetooth: BleBluetooth =
                    multipleBluetoothController.buildConnectingBle(bleDevice)
                bleBluetooth.bleGattCallback = bleGattCallback
                bleGattCallback?.onConnectCancel(bleDevice, true)
                bleBluetooth.bluetoothGatt
            } else {
                val bleBluetooth: BleBluetooth =
                    multipleBluetoothController.buildConnectingBle(bleDevice)
                val autoConnect: Boolean = strategy.mAutoConnect
                bleBluetooth.connect(context!!, autoConnect, strategy, bleGattCallback)
            }
        } else {
            if (multipleBluetoothController.isConnecting(bleDevice)) {
                multipleBluetoothController.cancelConnecting(bleDevice, true)
            }
            val bleBluetooth: BleBluetooth =
                multipleBluetoothController.buildConnectingBle(bleDevice)
            val autoConnect: Boolean = strategy.mAutoConnect
            return bleBluetooth.connect(
                context!!,
                autoConnect,
                strategy,
                bleGattCallback
            )
        }
    }

    /**
     * connect a device through its mac without scan,whether or not it has been connected
     *
     * @param mac
     * @param bleGattCallback
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun connect(
        mac: String,
        bleGattCallback: BleGattCallback?,
        strategy: BleConnectStrategy = bleConnectStrategy,
    ): BluetoothGatt? {
        val bleDevice = convertBleDevice(bluetoothAdapter?.getRemoteDevice(mac))
        return if (bleDevice == null) {
            bleGattCallback?.onConnectFail(
                null,
                BleException.OtherException(BleException.DEVICE_NULL, "Device is null")
            )
            null
        } else {
            connect(
                bleDevice,
                bleGattCallback,
                strategy
            )
        }
    }

    /**
     * notify
     *
     * @param bleDevice
     * @param uuidService
     * @param uuidNotify
     * @param callback
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun notify(
        bleDevice: BleDevice,
        uuidService: String,
        uuidNotify: String,
        callback: BleNotifyCallback?,
        useCharacteristicDescriptor: Boolean = false,
    ) {
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        if (bleBluetooth == null) {
            callback?.onNotifyFailure(
                bleDevice,
                null,
                BleException.OtherException(
                    BleException.DEVICE_NOT_CONNECT,
                    "This device not connect!"
                )
            )
        } else {
            bleBluetooth.buildNotifyOperator(uuidService, uuidNotify)
                .enableCharacteristicNotify(callback, uuidNotify, useCharacteristicDescriptor)
        }
    }

    /**
     * @param bleDevice
     * @param uuidIndicate
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun indicate(
        bleDevice: BleDevice,
        uuidService: String,
        uuidIndicate: String,
        callback: BleIndicateCallback?,
        useCharacteristicDescriptor: Boolean = false,
    ) {
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        if (bleBluetooth == null) {
            callback?.onIndicateFailure(
                bleDevice,
                null,
                BleException.OtherException(
                    BleException.DEVICE_NOT_CONNECT,
                    "This device not connect!"
                )
            )
        } else {
            bleBluetooth.buildIndicateOperator(uuidService, uuidIndicate)
                .enableCharacteristicIndicate(callback, uuidIndicate, useCharacteristicDescriptor)
        }
    }

    /**
     * stop notify, remove callback
     *
     * @param bleDevice
     * @param uuidService
     * @param uuidNotify
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun stopNotify(
        bleDevice: BleDevice,
        uuidService: String,
        uuidNotify: String,
        useCharacteristicDescriptor: Boolean = false,
    ): Boolean {
        val bleBluetooth =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice) ?: return false
        return bleBluetooth.buildNotifyOperator(uuidService, uuidNotify)
            .disableCharacteristicNotify(useCharacteristicDescriptor)
    }

    /**
     * stop indicate, remove callback
     *
     * @param bleDevice
     * @param uuidService
     * @param uuidIndicate
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun stopIndicate(
        bleDevice: BleDevice,
        uuidService: String,
        uuidIndicate: String,
        useCharacteristicDescriptor: Boolean = false,
    ): Boolean {
        val bleBluetooth =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice) ?: return false
        return bleBluetooth.buildIndicateOperator(uuidService, uuidIndicate)
            .disableCharacteristicIndicate(useCharacteristicDescriptor)
    }

    const val WRITE_TYPE_AUTO = BleOperator.WRITE_TYPE_DEFAULT

    const val WRITE_TYPE_DEFAULT = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

    const val WRITE_TYPE_NO_RESPONSE = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

    const val WRITE_TYPE_SIGNED = BluetoothGattCharacteristic.WRITE_TYPE_SIGNED

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [WRITE_TYPE_AUTO,
            WRITE_TYPE_DEFAULT,
            WRITE_TYPE_NO_RESPONSE,
            WRITE_TYPE_SIGNED]
    )
    internal annotation class BleWriteType

    /**
     * write
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun write(
        bleDevice: BleDevice,
        uuidService: String,
        uuidWrite: String,
        data: ByteArray?,
        split: Boolean = true,
        splitNum: Int = splitWriteNum,
        continueWhenLastFail: Boolean = false,
        intervalBetweenTwoPackage: Long = 0,
        callback: BleWriteCallback?,
        @BleWriteType writeType: Int = WRITE_TYPE_AUTO,
    ) {
        if (data == null) {
            BleLog.e("data is Null!")
            callback?.onWriteFailure(
                bleDevice,
                null,
                BleException.OtherException(BleException.DATA_NULL, "data is Null!"),
                justWrite = null
            )
            return
        }
        if (data.size > 20 && !split) {
            BleLog.w("Be careful: data's length beyond 20! Ensure MTU higher than 23, or use spilt write!")
        }
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        if (bleBluetooth == null) {
            callback?.onWriteFailure(
                bleDevice, null,
                BleException.OtherException(
                    BleException.DEVICE_NOT_CONNECT,
                    "This device is not connect!"
                ),
                justWrite = data
            )
        } else {
            if (split && data.size > splitNum) {
                SplitWriter(bleBluetooth.buildWriteOperator(uuidService, uuidWrite)).splitWrite(
                    data,
                    splitNum,
                    continueWhenLastFail,
                    intervalBetweenTwoPackage,
                    callback,
                    writeType
                )
            } else {
                bleBluetooth.buildWriteOperator(uuidService, uuidWrite)
                    .writeCharacteristic(data, callback, uuidWrite, writeType)
            }
        }
    }

    /**
     * read
     *
     * @param bleDevice
     * @param uuidService
     * @param uuidRead
     * @param callback
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun read(
        bleDevice: BleDevice,
        uuidService: String,
        uuidRead: String,
        callback: BleReadCallback?,
    ) {
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.buildReadOperator(uuidService, uuidRead)
            ?.readCharacteristic(callback, uuidRead)
            ?: callback?.onReadFailure(
                bleDevice,
                null,
                BleException.OtherException(
                    BleException.DEVICE_NOT_CONNECT,
                    "This device is not connected!"
                )
            )
    }

    /**
     * read Rssi
     *
     * @param bleDevice
     * @param callback
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun readRssi(
        bleDevice: BleDevice,
        callback: BleRssiCallback?,
    ) {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.buildRssiOperator()?.readRemoteRssi(callback)
            ?: callback?.onRssiFailure(
                bleDevice,
                BleException.OtherException(
                    BleException.DEVICE_NOT_CONNECT,
                    "This device is not connected!"
                )
            )
    }

    /**
     * set Mtu
     *
     * @param bleDevice
     * @param mtu
     * @param callback
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun setMtu(
        bleDevice: BleDevice,
        mtu: Int,
        callback: BleMtuChangedCallback?,
    ) {
        if (mtu > DEFAULT_MAX_MTU) {
            BleLog.e("requiredMtu should lower than 512 !")
            callback?.onSetMTUFailure(
                bleDevice,
                BleException.OtherException(description = "requiredMtu should lower than 512 !")
            )
            return
        }
        if (mtu < DEFAULT_MTU) {
            BleLog.e("requiredMtu should higher than 23 !")
            callback?.onSetMTUFailure(
                bleDevice,
                BleException.OtherException(description = "requiredMtu should higher than 23 !")
            )
            return
        }
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.buildMtuOperator()?.setMtu(mtu, callback)
            ?: callback?.onSetMTUFailure(
                bleDevice,
                BleException.OtherException(
                    BleException.DEVICE_NOT_CONNECT,
                    "This device is not connected!"
                )
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun readPhy(bleDevice: BleDevice): Boolean {
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        return bleBluetooth?.bluetoothGatt?.let {
            it.readPhy()
            true
        } ?: false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun setPreferredPhy(bleDevice: BleDevice, txPhy: Int, rxPhy: Int, phyOptions: Int): Boolean {
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        return bleBluetooth?.bluetoothGatt?.let {
            it.setPreferredPhy(txPhy, rxPhy, phyOptions)
            true
        } ?: false
    }

    /**
     * requestConnectionPriority
     *
     * @param connectionPriority Request a specific connection priority. Must be one of
     * [BluetoothGatt.CONNECTION_PRIORITY_BALANCED],
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]
     * or [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER].
     * @throws IllegalArgumentException If the parameters are outside of their
     * specified range.
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun requestConnectionPriority(bleDevice: BleDevice, connectionPriority: Int): Boolean {
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        return bleBluetooth?.bluetoothGatt?.requestConnectionPriority(connectionPriority)
            ?: false
    }

    /**
     * is support ble?
     *
     * @return
     */
    @JvmStatic
    fun isSupportBle(context: Context?): Boolean {
        if (context == null || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        context.getSystemService(Context.BLUETOOTH_SERVICE)?.let {
            return (it as BluetoothManager).adapter != null
        }
        return false
    }

    /**
     * judge Bluetooth is enable
     *
     * @return
     */
    @JvmStatic
    fun isBleEnable(context: Context): Boolean {
        if (!isSupportBle(context)) {
            return false
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter.isEnabled
    }

    /**
     * 全部取消，无回调
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun destroy() {
        releaseBleObserver()
        BleScanner.destroy()
        multipleBluetoothController.destroy()
    }

    /**
     * 全部取消，有scan和connect的回调
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun release() {
        releaseBleObserver()
        BleScanner.stopLeScan()
        BleScanner.destroy()
        multipleBluetoothController.cancelAllConnectingDevice()
        multipleBluetoothController.disconnectAllDevice()
    }

    /**
     * @param bleDevice
     * @return State of the profile connection. One of
     * [BluetoothProfile.STATE_CONNECTED],
     * [BluetoothProfile.STATE_CONNECTING],
     * [BluetoothProfile.STATE_DISCONNECTED],
     * [BluetoothProfile.STATE_DISCONNECTING]
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun getConnectState(bleDevice: BleDevice): Int {
        return bluetoothManager?.getConnectionState(bleDevice.device, BluetoothProfile.GATT)
            ?: BluetoothProfile.STATE_DISCONNECTED
    }

    fun isScanning(): Boolean {
        return BleScanner.mBleScanState == BleScanState.STATE_SCANNING
    }

    fun isConnected(bleDevice: BleDevice?): Boolean {
        return multipleBluetoothController.isConnectedDevice(bleDevice)
    }

    fun getAllConnectedDevice(): List<BleDevice> {
        return multipleBluetoothController.getConnectedDeviceList()
    }

    fun getConnectingDeviceList(): List<BleDevice> {
        return multipleBluetoothController.getConnectingDeviceList()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun isConnected(mac: String?): Boolean {
        val list: List<BleDevice> = getAllConnectedDevice()
        for (bleDevice in list) {
            if (bleDevice.mac == mac) {
                return true
            }
        }
        return false
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun disconnect(bleDevice: BleDevice?) {
        multipleBluetoothController.disconnect(bleDevice)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun disconnectAllDevice() {
        multipleBluetoothController.disconnectAllDevice()
    }

    fun scannerDestroy() {
        BleScanner.destroy()
    }

    fun cancelOrDisconnect(bleDevice: BleDevice?) {
        multipleBluetoothController.cancelOrDisconnect(bleDevice)
    }

    fun removeScanCallback() {
        BleScanner.bleScanCallback = null
    }

    fun cancelConnecting(bleDevice: BleDevice?) {
        multipleBluetoothController.cancelConnecting(bleDevice, false)
    }

    fun cancelAllConnectingDevice() {
        multipleBluetoothController.cancelAllConnectingDevice()
    }

    fun isConnecting(bleDevice: BleDevice?): Boolean {
        return multipleBluetoothController.isConnecting(bleDevice)
    }

    fun convertBleDevice(bluetoothDevice: BluetoothDevice?): BleDevice? {
        if (bluetoothDevice == null) return null
        return BleDevice(bluetoothDevice)
    }

    fun convertBleDevice(mac: String): BleDevice? {
        if (bluetoothAdapter == null) {
            BleLog.e("BleManager may not be initialized")
            return null
        }
        return try {
            convertBleDevice(bluetoothAdapter?.getRemoteDevice(mac))
        } catch (_: IllegalArgumentException) {
            null
        }

    }

    fun getBluetoothGatt(bleDevice: BleDevice?): BluetoothGatt? {
        return multipleBluetoothController.getConnectedBleBluetooth(bleDevice)?.bluetoothGatt
    }

    fun getConnectedDevice(mac: String?): BleDevice? {
        mac?.let {
            getAllConnectedDevice().forEach {
                if (it.mac == mac) {
                    return it
                }
            }
        }
        return null
    }

    fun getBluetoothGattServices(bleDevice: BleDevice?): List<BluetoothGattService>? {
        return multipleBluetoothController.getConnectedBleBluetooth(bleDevice)?.bluetoothGatt?.services
    }

    fun getBluetoothGattCharacteristics(service: BluetoothGattService?): List<BluetoothGattCharacteristic>? {
        return service?.characteristics
    }

    fun removeConnectGattCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)?.bleGattCallback = null
    }

    fun removeRssiCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)?.removeRssiOperator()
    }

    fun removeMtuChangedCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)?.removeMtuOperator()
    }

    fun removeNotifyCallback(bleDevice: BleDevice?, uuidNotify: String) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
            ?.removeNotifyOperator(uuidNotify)
    }

    fun removeIndicateCallback(bleDevice: BleDevice?, uuidIndicate: String) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
            ?.removeIndicateOperator(uuidIndicate)
    }

    fun removeWriteCallback(bleDevice: BleDevice?, uuidWrite: String) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
            ?.removeWriteOperator(uuidWrite)
    }

    fun removeReadCallback(bleDevice: BleDevice?, uuidRead: String) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
            ?.removeReadOperator(uuidRead)
    }

    fun clearCharacterCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)?.clearCharacterOperator()
    }

    private fun initBleObserver() {
        if (context == null) {
            BleLog.e("BleManager may not be initialized")
            return
        }
        if (bleObserver == null) {
            bleObserver = BluetoothChangedObserver()
            bleObserver!!.registerReceiver(context!!)
        }
    }

    @Synchronized
    fun setBleStateCallback(bleStatusCallback: BluetoothChangedObserver.BleStatusCallback) {
        initBleObserver()
        bleObserver?.bleStatusCallback = bleStatusCallback
    }

    private fun releaseBleObserver() {
        if (context == null) {
            BleLog.e("BleManager may not be initialized")
            return
        }
        if (bleObserver != null) {
            bleObserver!!.unregisterReceiver(context!!)
            bleObserver = null
        }
    }

    /**队列相关*****************************************************************/

    fun removeOperateQueue(
        bleDevice: BleDevice?,
        identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER,
    ) {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.removeOperateQueue(identifier)
    }

    fun removeOperatorFromQueue(
        bleDevice: BleDevice?,
        identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER,
        sequenceBleOperator: SequenceBleOperator,
    ): Boolean {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        return bleBluetooth?.removeOperatorFromQueue(identifier, sequenceBleOperator) ?: false
    }

    fun addOperatorToQueue(
        bleDevice: BleDevice?,
        identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER,
        sequenceBleOperator: SequenceBleOperator,
    ): Boolean {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        return bleBluetooth?.addOperatorToQueue(identifier, sequenceBleOperator) ?: false
    }

    fun clearQueue(
        bleDevice: BleDevice?,
        identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER,
    ) {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.clearQueue(identifier)
    }

    fun clearAllQueue(bleDevice: BleDevice?) {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.clearOperatorQueue()
    }

    fun pauseQueue(
        bleDevice: BleDevice?,
        identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER,
    ) {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.pauseQueue(identifier)
    }

    fun resume(bleDevice: BleDevice?, identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER) {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.resume(identifier)
    }
}