package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.callback.BleIndicateCallback
import com.huyuhui.fastble.callback.BleMtuChangedCallback
import com.huyuhui.fastble.callback.BleNotifyCallback
import com.huyuhui.fastble.callback.BleOperateCallback
import com.huyuhui.fastble.callback.BleReadCallback
import com.huyuhui.fastble.callback.BleRssiCallback
import com.huyuhui.fastble.callback.BleWriteCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleCoroutineExceptionHandler
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.utils.BleLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
internal class BleOperator(private val bleBluetooth: BleBluetooth) :
    CoroutineScope by CoroutineScope(SupervisorJob(bleBluetooth.coroutineContext.job) + Dispatchers.Main.immediate
            + BleCoroutineExceptionHandler { _, throwable ->
        BleLog.e(
            "Bluetooth operation: a coroutine error has occurred. ${throwable.message}\n " +
                    "Device:${bleBluetooth.bleDevice} \n"
        )
    }) {
    companion object {
        @JvmStatic
        val UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR =
            "00002902-0000-1000-8000-00805f9b34fb"


        const val WRITE_TYPE_DEFAULT = -1
    }

    /**
     * 操作的数据
     */
    val bleDevice: BleDevice
        get() = bleBluetooth.bleDevice
    var data: ByteArray? = null
        private set
    var operateCallback: BleOperateCallback? = null
        private set
    private val timeOutTask = TimeoutTask(
        BleManager.operateTimeout, object : TimeoutTask.OnResultCallBack {
            override fun onError(task: TimeoutTask, e: Throwable?, isActive: Boolean) {
                super.onError(task, e, isActive)
                operateCallback?.onTimeOutFailure(
                    bleDevice,
                    mCharacteristic,
                    BleException.TimeoutException(),
                    data
                )
            }
        }
    )
    private val mBluetoothGatt: BluetoothGatt?
        get() = bleBluetooth.bluetoothGatt
    var mGattService: BluetoothGattService? = null
        private set
    var mCharacteristic: BluetoothGattCharacteristic? = null
        private set


    private fun withUUID(serviceUUID: UUID, characteristicUUID: UUID): BleOperator {
        mGattService = mBluetoothGatt?.getService(serviceUUID)
        mCharacteristic = mGattService?.getCharacteristic(characteristicUUID)
        return this
    }

    fun withUUIDString(serviceUUID: String, characteristicUUID: String): BleOperator {
        return withUUID(fromUUID(serviceUUID), fromUUID(characteristicUUID))
    }

    private fun fromUUID(uuid: String): UUID {
        return UUID.fromString(uuid)
    }

    fun removeTimeOut() {
        timeOutTask.success()
    }
    /*------------------------------- main operation ----------------------------------- */
    /**
     * notify
     */
    fun enableCharacteristicNotify(
        bleNotifyCallback: BleNotifyCallback?,
        uuidNotify: String,
        useCharacteristicDescriptor: Boolean,
    ) {
        if (mCharacteristic != null && mCharacteristic!!.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
            operateCallback = bleNotifyCallback
            bleBluetooth.addNotifyOperator(uuidNotify, this)
            launch {
                timeOutTask.start()
            }
            setCharacteristicNotification(
                mBluetoothGatt,
                mCharacteristic,
                true,
                bleNotifyCallback,
                useCharacteristicDescriptor
            )
        } else {
            launch {
                bleNotifyCallback?.onNotifyFailure(
                    bleDevice,
                    mCharacteristic,
                    BleException.OtherException(
                        BleException.CHARACTERISTIC_NOT_SUPPORT,
                        "this characteristic not support notify!"
                    )
                )
            }
        }
    }

    /**
     * stop notify
     */
    fun disableCharacteristicNotify(useCharacteristicDescriptor: Boolean): Boolean {
        return if (mCharacteristic != null
            && mCharacteristic!!.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0
        ) {
            setCharacteristicNotification(
                mBluetoothGatt, mCharacteristic, false, null, useCharacteristicDescriptor
            )
        } else {
            false
        }
    }


    @Suppress("DEPRECATION")
    private fun setCharacteristicNotification(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        enable: Boolean,
        bleNotifyCallback: BleNotifyCallback?,
        useCharacteristicDescriptor: Boolean,
    ): Boolean {
        if (gatt == null || characteristic == null) {
            removeTimeOut()
            bleNotifyCallback?.onNotifyFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(
                    BleException.GATT_NULL,
                    "gatt or characteristic equal null"
                )
            )
            return false
        }
        val success1 = gatt.setCharacteristicNotification(characteristic, enable)
        if (!success1) {
            removeTimeOut()
            bleNotifyCallback?.onNotifyFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_ERROR,
                    "gatt setCharacteristicNotification fail"
                )
            )
            return false
        }
        val descriptor: BluetoothGattDescriptor? =
            if (useCharacteristicDescriptor) characteristic.getDescriptor(characteristic.uuid) else
                characteristic.getDescriptor(fromUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR))
        return if (descriptor == null) {
            removeTimeOut()
            bleNotifyCallback?.onNotifyFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(BleException.DESCRIPTOR_NULL, "descriptor equals null")
            )
            false
        } else {
            data =
                if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            descriptor.value = data!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val state = gatt.writeDescriptor(
                    descriptor,
                    data!!
                )
                val success2 = state == BluetoothStatusCodes.SUCCESS
                if (!success2) {
                    removeTimeOut()
                    bleNotifyCallback?.onNotifyFailure(
                        bleDevice,
                        characteristic,
                        BleException.OtherException(
                            BleException.DESCRIPTOR_ERROR,
                            "gatt writeDescriptor fail"
                        )
                    )
                }
                success2
            } else {
                val success2 = gatt.writeDescriptor(descriptor)
                if (!success2) {
                    removeTimeOut()
                    bleNotifyCallback?.onNotifyFailure(
                        bleDevice,
                        characteristic,
                        BleException.OtherException(
                            BleException.DESCRIPTOR_ERROR,
                            "gatt writeDescriptor fail"
                        )
                    )
                }
                success2
            }
        }
    }

    fun enableCharacteristicIndicate(
        bleIndicateCallback: BleIndicateCallback?,
        uuidIndicate: String,
        useCharacteristicDescriptor: Boolean,
    ) {
        if (mCharacteristic != null
            && mCharacteristic!!.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0
        ) {
            operateCallback = bleIndicateCallback
            bleBluetooth.addIndicateOperator(uuidIndicate, this)
            launch {
                timeOutTask.start()
            }
            setCharacteristicIndication(
                mBluetoothGatt, mCharacteristic,
                true, bleIndicateCallback, useCharacteristicDescriptor
            )
        } else {
            bleIndicateCallback?.onIndicateFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_NOT_SUPPORT,
                    "this characteristic not support indicate!"
                )
            )
        }
    }


    /**
     * stop indicate
     */
    fun disableCharacteristicIndicate(useCharacteristicDescriptor: Boolean): Boolean {
        return if (mCharacteristic != null
            && mCharacteristic!!.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0
        ) {
            setCharacteristicIndication(
                mBluetoothGatt, mCharacteristic,
                false, null, useCharacteristicDescriptor
            )
        } else {
            false
        }
    }

    /**
     * indicate setting
     */
    @Suppress("DEPRECATION")
    private fun setCharacteristicIndication(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        enable: Boolean,
        bleIndicateCallback: BleIndicateCallback?,
        useCharacteristicDescriptor: Boolean,
    ): Boolean {
        if (gatt == null || characteristic == null) {
            removeTimeOut()
            bleIndicateCallback?.onIndicateFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(
                    BleException.GATT_NULL,
                    "gatt or characteristic equal null"
                )
            )
            return false
        }
        val success1 = gatt.setCharacteristicNotification(characteristic, enable)
        if (!success1) {
            removeTimeOut()
            bleIndicateCallback?.onIndicateFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_ERROR,
                    "gatt setCharacteristicNotification fail"
                )
            )
            return false
        }
        val descriptor: BluetoothGattDescriptor? =
            if (useCharacteristicDescriptor) characteristic.getDescriptor(characteristic.uuid) else
                characteristic.getDescriptor(fromUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR))
        return if (descriptor == null) {
            removeTimeOut()
            bleIndicateCallback?.onIndicateFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(BleException.DESCRIPTOR_NULL, "descriptor equals null")
            )
            false
        } else {
            data =
                if (enable) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            descriptor.value = data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val state = gatt.writeDescriptor(
                    descriptor,
                    data!!
                )
                val success2 = state == BluetoothStatusCodes.SUCCESS
                if (!success2) {
                    removeTimeOut()
                    bleIndicateCallback?.onIndicateFailure(
                        bleDevice,
                        mCharacteristic,
                        BleException.OtherException(
                            BleException.DESCRIPTOR_ERROR,
                            "gatt writeDescriptor fail"
                        )
                    )
                }
                success2
            } else {
                val success2 = gatt.writeDescriptor(descriptor)
                if (!success2) {
                    removeTimeOut()
                    bleIndicateCallback?.onIndicateFailure(
                        bleDevice,
                        mCharacteristic,
                        BleException.OtherException(
                            BleException.DESCRIPTOR_ERROR,
                            "gatt writeDescriptor fail"
                        )
                    )
                }
                success2
            }
        }
    }

    @Suppress("DEPRECATION")
    fun writeCharacteristic(
        data: ByteArray?,
        bleWriteCallback: BleWriteCallback?,
        uuidWrite: String,
        writeType: Int,
    ) {
        this.data = data
        if (data == null || data.isEmpty()) {
            bleWriteCallback?.onWriteFailure(
                bleDevice, mCharacteristic,
                BleException.OtherException(
                    BleException.DATA_NULL,
                    "the data to be written is empty"
                ),
                justWrite = data
            )
            return
        }
        if (mCharacteristic == null
            || mCharacteristic!!.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == 0
        ) {
            bleWriteCallback?.onWriteFailure(
                bleDevice, mCharacteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_NOT_SUPPORT,
                    "this characteristic not support write!"
                ),
                justWrite = data
            )
            return
        }
        val finalWriteType = if (writeType == WRITE_TYPE_DEFAULT) {
            if (mCharacteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else if (mCharacteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
            }
        } else {
            writeType
        }
        mCharacteristic!!.writeType = finalWriteType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            operateCallback = bleWriteCallback
            launch {
                timeOutTask.start()
            }
            bleBluetooth.addWriteOperator(uuidWrite, this)
            mBluetoothGatt!!.writeCharacteristic(mCharacteristic!!, data, finalWriteType)
            //这里会触发一次，onCharacteristicWrite里面还会触发一次
//            val status = mBluetoothGatt!!.writeCharacteristic(mCharacteristic!!, data, writeType)
//            if (status != BluetoothStatusCodes.SUCCESS) {
//                removeTimeOut()
//                bleWriteCallback?.onWriteFailure(
//                    BleException.OtherException("Updates the locally stored value of this characteristic fail"),
//                    justWrite = data
//                )
//            }
        } else {
            if (mCharacteristic!!.setValue(data)) {
                operateCallback = bleWriteCallback
                launch {
                    timeOutTask.start()
                }
                bleBluetooth.addWriteOperator(uuidWrite, this)
                mBluetoothGatt!!.writeCharacteristic(mCharacteristic)
//                if (!mBluetoothGatt!!.writeCharacteristic(mCharacteristic)) {
//                    removeTimeOut()
//                    bleWriteCallback?.onWriteFailure(
//                        BleException.OtherException("gatt writeCharacteristic fail"),
//                        justWrite = data
//                    )
//                }
            } else {
                bleWriteCallback?.onWriteFailure(
                    bleDevice, mCharacteristic,
                    BleException.OtherException(
                        BleException.CHARACTERISTIC_ERROR,
                        "Updates the locally stored value of this characteristic fail"
                    ),
                    justWrite = data
                )
            }
        }
    }

    /**
     * read
     */
    fun readCharacteristic(bleReadCallback: BleReadCallback?, uuidRead: String) {
        if (mCharacteristic != null
            && mCharacteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_READ > 0
        ) {
            operateCallback = bleReadCallback
            launch {
                timeOutTask.start()
            }
            bleBluetooth.addReadOperator(uuidRead, this)
            if (!mBluetoothGatt!!.readCharacteristic(mCharacteristic)) {
                removeTimeOut()
                bleReadCallback?.onReadFailure(
                    bleDevice,
                    mCharacteristic,
                    BleException.OtherException(
                        BleException.CHARACTERISTIC_ERROR,
                        "gatt readCharacteristic fail"
                    )
                )
            }
        } else {
            bleReadCallback?.onReadFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_NOT_SUPPORT,
                    "this characteristic not support read!"
                )
            )
        }
    }

    /**
     * rssi
     */
    fun readRemoteRssi(bleRssiCallback: BleRssiCallback?) {
        if (mBluetoothGatt == null) {
            bleRssiCallback?.onRssiFailure(
                bleDevice,
                BleException.OtherException(BleException.GATT_NULL, "gatt is null")
            )
        } else {
            launch {
                timeOutTask.start()
            }
            operateCallback = bleRssiCallback
            bleBluetooth.setRssiOperator(this)
            if (!mBluetoothGatt!!.readRemoteRssi()) {
                removeTimeOut()
                bleRssiCallback?.onRssiFailure(
                    bleDevice,
                    BleException.OtherException(description = "gatt readRemoteRssi fail")
                )
            }
        }
    }

    /**
     * set mtu
     */
    fun setMtu(requiredMtu: Int, bleMtuChangedCallback: BleMtuChangedCallback?) {
        if (mBluetoothGatt == null) {
            bleMtuChangedCallback?.onSetMTUFailure(
                bleDevice,
                BleException.OtherException(description = "gatt requestMtu fail")
            )
        } else {
            launch {
                timeOutTask.start()
            }
            operateCallback = bleMtuChangedCallback
            bleBluetooth.setMtuOperator(this)
            if (!mBluetoothGatt!!.requestMtu(requiredMtu)) {
                removeTimeOut()
                bleMtuChangedCallback?.onSetMTUFailure(
                    bleDevice,
                    BleException.OtherException(description = "gatt requestMtu fail")
                )
            }
        }
    }

    fun destroy() {
        data = null
        operateCallback = null
        timeOutTask.onTimeoutResultCallBack = null
        cancel()
    }
}