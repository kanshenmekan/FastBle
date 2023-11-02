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
    private const val DEFAULT_CONNECT_RETRY_COUNT = 0
    private const val DEFAULT_CONNECT_RETRY_INTERVAL: Long = 2000
    private const val DEFAULT_MTU = 23
    private const val DEFAULT_MAX_MTU = 512
    private const val DEFAULT_WRITE_DATA_SPLIT_COUNT = 20
    private const val DEFAULT_CONNECT_OVER_TIME: Long = 10000

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
     * connect retry count
     */
    var reConnectCount = DEFAULT_CONNECT_RETRY_COUNT
        private set

    /**
     * connect retry interval
     */
    var reConnectInterval = DEFAULT_CONNECT_RETRY_INTERVAL
        private set

    /**
     * operate split Write Num
     */
    var splitWriteNum = DEFAULT_WRITE_DATA_SPLIT_COUNT
        set(value) {
            if (value > 0) {
                field = value
            }
        }

    /**
     * Get operate connect Over Time
     *
     */
    var connectOverTime = DEFAULT_CONNECT_OVER_TIME
        set(value) {
            field = if (value <= 0) {
                100
            } else {
                value
            }
        }

    lateinit var context: Application
        private set
    lateinit var bleScanRuleConfig: BleScanRuleConfig
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
        bleScanRuleConfig = BleScanRuleConfig.Builder().build()
        multipleBluetoothController = MultipleBluetoothController()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    fun scan(bleScanCallback: BleScanCallback) {
        if (!isBleEnable(context)) {
            bleScanCallback.onScanStarted(false)
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
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun connect(bleDevice: BleDevice, bleGattCallback: BleGattCallback): BluetoothGatt? {
        if (!isBleEnable(context)) {
            BleLog.e("Bluetooth not enable!")
            bleGattCallback.onConnectFail(
                bleDevice,
                BleException.OtherException("Bluetooth not enable!")
            )
            return null
        }
        if (Looper.myLooper() == null || Looper.myLooper() != Looper.getMainLooper()) {
            BleLog.w("Be careful: currentThread is not MainThread!")
        }
        if (bleDevice.device == null) {
            bleGattCallback.onConnectFail(
                bleDevice,
                BleException.OtherException("Not Found Device Exception Occurred!")
            )
        } else {
            val bleBluetooth: BleBluetooth =
                multipleBluetoothController.buildConnectingBle(bleDevice)
            val autoConnect: Boolean = bleScanRuleConfig.mAutoConnect
            return bleBluetooth.connect(context, autoConnect, bleGattCallback)
        }
        return null
    }

    /**
     * notify
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @param useCharacteristicDescriptor
     * @param callback
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun notify(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_notify: String,
        useCharacteristicDescriptor: Boolean = false,
        callback: BleNotifyCallback
    ) {
        val bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice)
        if (bleBluetooth == null) {
            callback.onNotifyFailure(BleException.OtherException("This device not connect!"))
        } else {
            bleBluetooth.newOperator(uuid_service, uuid_notify)
                .enableCharacteristicNotify(callback, uuid_notify, useCharacteristicDescriptor)
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
        useCharacteristicDescriptor: Boolean,
        callback: BleIndicateCallback
    ) {
        val bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice)
        if (bleBluetooth == null) {
            callback.onIndicateFailure(BleException.OtherException("This device not connect!"))
        } else {
            bleBluetooth.newOperator(uuid_service, uuid_indicate)
                .enableCharacteristicIndicate(callback, uuid_indicate, useCharacteristicDescriptor)
        }
    }

    /**
     * stop notify, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @param useCharacteristicDescriptor
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun stopNotify(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_notify: String,
        useCharacteristicDescriptor: Boolean = false
    ): Boolean {
        val bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice) ?: return false
        val success: Boolean = bleBluetooth.newOperator(uuid_service, uuid_notify)
            .disableCharacteristicNotify(useCharacteristicDescriptor)
        if (success) {
            bleBluetooth.removeNotifyOperator(uuid_notify)
        }
        return success
    }

    /**
     * stop indicate, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_indicate
     * @param useCharacteristicDescriptor
     * @return
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun stopIndicate(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_indicate: String,
        useCharacteristicDescriptor: Boolean = false
    ): Boolean {
        val bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice) ?: return false
        val success: Boolean = bleBluetooth.newOperator(uuid_service, uuid_indicate)
            .disableCharacteristicIndicate(useCharacteristicDescriptor)
        if (success) {
            bleBluetooth.removeIndicateOperator(uuid_indicate)
        }
        return success
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun write(
        bleDevice: BleDevice?,
        uuid_service: String,
        uuid_write: String,
        data: ByteArray?,
        split: Boolean = true,
        sendNextWhenLastSuccess: Boolean = true,
        intervalBetweenTwoPackage: Long = 0,
        callback: BleWriteCallback,
        writeType: Int = BleOperator.WRITE_TYPE_DEFAULT
    ) {
        if (data == null) {
            BleLog.e("data is Null!")
            callback.onWriteFailure(BleException.OtherException("data is Null!"), justWrite = null)
            return
        }
        if (data.size > 20 && !split) {
            BleLog.w("Be careful: data's length beyond 20! Ensure MTU higher than 23, or use spilt write!")
        }
        val bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice)
        if (bleBluetooth == null) {
            callback.onWriteFailure(
                BleException.OtherException("This device not connect!"),
                justWrite = data
            )
        } else {
            if (split && data.size > splitWriteNum) {
                SplitWriter(bleBluetooth.newOperator(uuid_service, uuid_write)).splitWrite(
                    data, sendNextWhenLastSuccess, intervalBetweenTwoPackage, callback, writeType
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
        val bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice)
        bleBluetooth?.newOperator(uuid_service, uuid_read)
            ?.readCharacteristic(callback, uuid_read)
            ?: callback.onReadFailure(BleException.OtherException("This device is not connected!"))
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
        requireNotNull(callback) { "BleRssiCallback can not be Null!" }
        val bleBluetooth: BleBluetooth? = multipleBluetoothController.getBleBluetooth(bleDevice)
        bleBluetooth?.newOperator()?.readRemoteRssi(callback)
            ?: callback.onRssiFailure(BleException.OtherException("This device is not connected!"))
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
        requireNotNull(callback) { "BleMtuChangedCallback can not be Null!" }
        if (mtu > DEFAULT_MAX_MTU) {
            BleLog.e("requiredMtu should lower than 512 !")
            callback.onSetMTUFailure(BleException.OtherException("requiredMtu should lower than 512 !"))
            return
        }
        if (mtu < DEFAULT_MTU) {
            BleLog.e("requiredMtu should higher than 23 !")
            callback.onSetMTUFailure(BleException.OtherException("requiredMtu should higher than 23 !"))
            return
        }
        val bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice)
        bleBluetooth?.newOperator()?.setMtu(mtu, callback)
            ?: callback.onSetMTUFailure(BleException.OtherException("This device is not connected!"))
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
        val bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice)
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

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun destroy() {
        BleScanner.stopLeScan()
        multipleBluetoothController.destroy()
    }

    fun setReConnectCount(count: Int, interval: Long): BleManager {
        reConnectCount = if (count > 10) 10 else count
        reConnectInterval = if (interval < 0) 0 else interval
        return this
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
        return getConnectState(bleDevice) == BluetoothProfile.STATE_CONNECTED
    }

    fun getAllConnectedDevice(): List<BleDevice> {
        return multipleBluetoothController.getDeviceList()
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

    fun convertBleDevice(bluetoothDevice: BluetoothDevice): BleDevice {
        return BleDevice(null, device = bluetoothDevice)
    }

    fun convertBleDevice(scanResult: ScanResult): BleDevice {
        return BleDevice(scanResult)
    }

    fun getBluetoothGatt(bleDevice: BleDevice?): BluetoothGatt? {
        return multipleBluetoothController.getBleBluetooth(bleDevice)?.bluetoothGatt
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
        return multipleBluetoothController.getBleBluetooth(bleDevice)?.bluetoothGatt?.services
    }

    fun getBluetoothGattCharacteristics(service: BluetoothGattService?): List<BluetoothGattCharacteristic>? {
        return service?.characteristics
    }

    fun removeConnectGattCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getBleBluetooth(bleDevice)?.bleGattCallback = null
    }

    fun removeRssiCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getBleBluetooth(bleDevice)?.removeRssiOperator()
    }

    fun removeMtuChangedCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getBleBluetooth(bleDevice)?.removeMtuOperator()
    }

    fun removeNotifyCallback(bleDevice: BleDevice?, uuid_notify: String) {
        multipleBluetoothController.getBleBluetooth(bleDevice)?.removeNotifyOperator(uuid_notify)
    }

    fun removeIndicateCallback(bleDevice: BleDevice?, uuid_indicate: String) {
        multipleBluetoothController.getBleBluetooth(bleDevice)
            ?.removeIndicateOperator(uuid_indicate)
    }

    fun removeWriteCallback(bleDevice: BleDevice?, uuid_write: String) {
        multipleBluetoothController.getBleBluetooth(bleDevice)?.removeWriteOperator(uuid_write)
    }

    fun removeReadCallback(bleDevice: BleDevice?, uuid_read: String) {
        multipleBluetoothController.getBleBluetooth(bleDevice)?.removeReadOperator(uuid_read)
    }

    fun clearCharacterCallback(bleDevice: BleDevice?) {
        multipleBluetoothController.getBleBluetooth(bleDevice)?.clearCharacterOperator()
    }
}