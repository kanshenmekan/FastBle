package com.hyh.ble

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.hyh.ble.bluetooth.BleBluetooth
import com.hyh.ble.bluetooth.BleOperator
import com.hyh.ble.bluetooth.MultipleBluetoothController
import com.hyh.ble.bluetooth.SplitWriter
import com.hyh.ble.callback.BleGattCallback
import com.hyh.ble.callback.BleIndicateCallback
import com.hyh.ble.callback.BleMtuChangedCallback
import com.hyh.ble.callback.BleNotifyCallback
import com.hyh.ble.callback.BleReadCallback
import com.hyh.ble.callback.BleRssiCallback
import com.hyh.ble.callback.BleScanCallback
import com.hyh.ble.callback.BleWriteCallback
import com.hyh.ble.common.BleConnectStrategy
import com.hyh.ble.common.BluetoothChangedObserver
import com.hyh.ble.data.BleDevice
import com.hyh.ble.data.BleScanState
import com.hyh.ble.exception.BleException
import com.hyh.ble.scan.BleScanRuleConfig
import com.hyh.ble.scan.BleScanner
import com.hyh.ble.utils.BleLog


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


    lateinit var context: Application
        private set
    var bleScanRuleConfig: BleScanRuleConfig = BleScanRuleConfig.Builder().build()
    var bleConnectStrategy: BleConnectStrategy = BleConnectStrategy()
    val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter


    //多设备连接管理
    internal lateinit var multipleBluetoothController: MultipleBluetoothController
        private set

    var bluetoothManager: BluetoothManager? = null
        private set

    fun init(app: Application) {
        context = app
        if (isSupportBle(app)) {
            bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        }
        multipleBluetoothController = MultipleBluetoothController()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    fun scan(bleScanCallback: BleScanCallback?) {
        if (!isBleEnable(context)) {
            bleScanCallback?.onScanStarted(false)
            return
        }
        BleScanner.bleScanCallback = bleScanCallback
        BleScanner.startLeScan()
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
        bleDevice: BleDevice?,
        bleGattCallback: BleGattCallback?,
        strategy: BleConnectStrategy = bleConnectStrategy
    ): BluetoothGatt? {
        if (!isSupportBle(context)) {
            bleGattCallback?.onConnectFail(
                bleDevice,
                BleException.OtherException(BleException.NOT_SUPPORT_BLE, "Bluetooth not support!")
            )
            return null
        }
        if (!isBleEnable(context)) {
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
        if (bleDevice == null) {
            bleGattCallback?.onConnectFail(
                null,
                BleException.OtherException(BleException.DEVICE_NULL, "Device is null")
            )
        }
        if (bleDevice!!.device == null) {
            bleGattCallback?.onConnectFail(
                bleDevice,
                BleException.OtherException(
                    BleException.DEVICE_NULL,
                    "Not Found Device Exception Occurred!"
                )
            )
        } else {
            if (multipleBluetoothController.isConnectedDevice(bleDevice)) {
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
                    val autoConnect: Boolean = bleScanRuleConfig.mAutoConnect
                    bleBluetooth.connect(context, autoConnect, bleConnectStrategy, bleGattCallback)
                }
            } else {
                if (multipleBluetoothController.isConnecting(bleDevice)) {
                    multipleBluetoothController.cancelConnecting(bleDevice, true)
                }
                val bleBluetooth: BleBluetooth =
                    multipleBluetoothController.buildConnectingBle(bleDevice)
                val autoConnect: Boolean = bleScanRuleConfig.mAutoConnect
                return bleBluetooth.connect(
                    context,
                    autoConnect,
                    bleConnectStrategy,
                    bleGattCallback
                )
            }
        }
        return null
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
        mac: String?,
        bleGattCallback: BleGattCallback?,
        strategy: BleConnectStrategy = bleConnectStrategy
    ): BluetoothGatt? {
        return connect(convertBleDevice(bluetoothAdapter?.getRemoteDevice(mac)), bleGattCallback)
    }

    /**
     * notify
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @param callback
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun notify(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_notify: String,
        callback: BleNotifyCallback?
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
            bleBluetooth.newOperator(uuid_service, uuid_notify)
                .enableCharacteristicNotify(callback, uuid_notify)
        }
    }

    /**
     * @param bleDevice
     * @param uuid_indicate
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun indicate(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_indicate: String,
        callback: BleIndicateCallback?
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
            bleBluetooth.newOperator(uuid_service, uuid_indicate)
                .enableCharacteristicIndicate(callback, uuid_indicate)
        }
    }

    /**
     * stop notify, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun stopNotify(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_notify: String,
    ): Boolean {
        val bleBluetooth =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice) ?: return false
        return bleBluetooth.newOperator(uuid_service, uuid_notify)
            .disableCharacteristicNotify()
    }

    /**
     * stop indicate, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_indicate
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun stopIndicate(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_indicate: String,
    ): Boolean {
        val bleBluetooth =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice) ?: return false
        return bleBluetooth.newOperator(uuid_service, uuid_indicate)
            .disableCharacteristicIndicate()
    }

    /**
     * write
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun write(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_write: String,
        data: ByteArray?,
        split: Boolean = true,
        continueWhenLastFail: Boolean = false,
        intervalBetweenTwoPackage: Long = 0,
        callback: BleWriteCallback?,
        writeType: Int = BleOperator.WRITE_TYPE_DEFAULT
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
            if (split && data.size > splitWriteNum) {
                SplitWriter(bleBluetooth.newOperator(uuid_service, uuid_write)).splitWrite(
                    data, continueWhenLastFail, intervalBetweenTwoPackage, callback, writeType
                )
            } else {
                bleBluetooth.newOperator(uuid_service, uuid_write)
                    .writeCharacteristic(data, callback, uuid_write, writeType)
            }
        }
    }

    /**
     * read
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_read
     * @param callback
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun read(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_read: String,
        callback: BleReadCallback?
    ) {
        requireNotNull(callback) { "BleReadCallback can not be Null!" }
        val bleBluetooth = multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.newOperator(uuid_service, uuid_read)
            ?.readCharacteristic(callback, uuid_read)
            ?: callback.onReadFailure(
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
        bleDevice: BleDevice?,
        callback: BleRssiCallback?
    ) {
        val bleBluetooth: BleBluetooth? =
            multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
        bleBluetooth?.newOperator()?.readRemoteRssi(callback)
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
        bleDevice: BleDevice?,
        mtu: Int,
        callback: BleMtuChangedCallback?
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
        bleBluetooth?.newOperator()?.setMtu(mtu, callback)
            ?: callback?.onSetMTUFailure(
                bleDevice,
                BleException.OtherException(
                    BleException.DEVICE_NOT_CONNECT,
                    "This device is not connected!"
                )
            )
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
    fun requestConnectionPriority(bleDevice: BleDevice?, connectionPriority: Int): Boolean {
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
    fun getConnectState(bleDevice: BleDevice?): Int {
        return if (bleDevice != null) {
            bluetoothManager?.getConnectionState(bleDevice.device, BluetoothProfile.GATT)
                ?: BluetoothProfile.STATE_DISCONNECTED
        } else {
            BluetoothProfile.STATE_DISCONNECTED
        }
    }

    fun isScanning(): Boolean {
        return BleScanner.mBleScanState == BleScanState.STATE_SCANNING
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun isConnected(bleDevice: BleDevice?): Boolean {
        return multipleBluetoothController.isConnectedDevice(bleDevice)
    }

    fun getAllConnectedDevice(): List<BleDevice> {
        return multipleBluetoothController.getConnectedDeviceList()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun isConnected(mac: String?): Boolean {
        val list: List<BleDevice> = getAllConnectedDevice()
        for (bleDevice in list) {
            if (bleDevice.mac.equals(mac)) {
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

    fun removeScanCallback() {
        BleScanner.bleScanCallback = null
    }

    fun cancelConnecting(bleDevice: BleDevice?) {
        multipleBluetoothController.cancelConnecting(bleDevice, false)
    }

    fun cancelAllConnectingDevice() {
        multipleBluetoothController.cancelAllConnectingDevice()
    }

    fun isConnecting(bleDevice: BleDevice?) {
        multipleBluetoothController.isConnecting(bleDevice)
    }

    fun convertBleDevice(bluetoothDevice: BluetoothDevice?): BleDevice? {
        if (bluetoothDevice == null) return null
        return BleDevice(null, device = bluetoothDevice)
    }

    fun convertBleDevice(scanResult: ScanResult): BleDevice {
        return BleDevice(scanResult)
    }

    fun convertBleDevice(mac: String): BleDevice? {
        bluetoothAdapter?.getRemoteDevice(mac)?.let {
            return convertBleDevice(it)
        }
        return null
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

    fun removeNotifyCallback(bleDevice: BleDevice?, uuid_notify: String) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
            ?.removeNotifyOperator(uuid_notify)
    }

    fun removeIndicateCallback(bleDevice: BleDevice?, uuid_indicate: String) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
            ?.removeIndicateOperator(uuid_indicate)
    }

    fun removeWriteCallback(bleDevice: BleDevice?, uuid_write: String) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
            ?.removeWriteOperator(uuid_write)
    }

    fun removeReadCallback(bleDevice: BleDevice?, uuid_read: String) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)
            ?.removeReadOperator(uuid_read)
    }

    fun clearCharacterCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getConnectedBleBluetooth(bleDevice)?.clearCharacterOperator()
    }

    private fun initBleObserver() {
        if (bleObserver == null) {
            bleObserver = BluetoothChangedObserver()
            bleObserver!!.registerReceiver(context)
        }
    }

    @Synchronized
    fun setBleStateCallback(bleStatusCallback: BluetoothChangedObserver.BleStatusCallback) {
        initBleObserver()
        bleObserver?.bleStatusCallback = bleStatusCallback
    }

    private fun releaseBleObserver() {
        if (bleObserver != null) {
            bleObserver!!.unregisterReceiver(context)
            bleObserver = null
        }
    }
}