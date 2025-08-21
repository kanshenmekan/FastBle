package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import com.huyuhui.fastble.callback.BleWriteCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.exception.BleException
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
internal class BleWriteOperator(bleBluetooth: BleBluetooth) : BleOperator(bleBluetooth) {
    var bleWriteCallback: BleWriteCallback? = null
        private set

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
            this.bleWriteCallback = bleWriteCallback
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
                this.bleWriteCallback = bleWriteCallback
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

    override fun onTimeout(
        task: TimeoutTask,
        e: Throwable?,
        isActive: Boolean
    ) {
        bleWriteCallback?.onWriteFailure(bleDevice, mCharacteristic, BleException.TimeoutException(), justWrite = data)
    }

    override fun destroy() {
        super.destroy()
        bleWriteCallback = null
    }
}