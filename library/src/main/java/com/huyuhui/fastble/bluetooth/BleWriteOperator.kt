package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import com.huyuhui.fastble.callback.BleWriteCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.exception.BleException

@SuppressLint("MissingPermission")
internal class BleWriteOperator : BleCharacteristicOperator {

    constructor(
        bleBluetooth: BleBluetooth,
        timeout: Long,
        uuidService: String,
        uuidCharacteristic: String
    ) : super(bleBluetooth, timeout, uuidService, uuidCharacteristic)

    constructor(
        bleBluetooth: BleBluetooth,
        timeout: Long,
        characteristic: BluetoothGattCharacteristic?
    ) : super(bleBluetooth, timeout, characteristic)

    var bleWriteCallback: BleWriteCallback? = null
        private set

    var data: ByteArray? = null
        private set

    @Suppress("DEPRECATION")
    fun writeCharacteristic(
        data: ByteArray?,
        bleWriteCallback: BleWriteCallback?,
        writeType: Int,
    ) {
        this.data = data
        if (data == null || data.isEmpty()) {
            bleWriteCallback?.onWriteFailure(
                bleDevice, gattCharacteristic,
                BleException.OtherException(
                    BleException.DATA_NULL,
                    "the data to be written is empty"
                ),
                justWrite = data
            )
            return
        }
        val characteristic = gattCharacteristic
        if (characteristic == null) {
            bleWriteCallback?.onWriteFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_ERROR,
                    "characteristic is null!"
                ), justWrite = data
            )
            return
        }
        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == 0
        ) {
            bleWriteCallback?.onWriteFailure(
                bleDevice, characteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_NOT_SUPPORT,
                    "this characteristic not support write!"
                ),
                justWrite = data
            )
            return
        }
        val finalWriteType = if (writeType == WRITE_TYPE_DEFAULT) {
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
            }
        } else {
            writeType
        }
        characteristic.writeType = finalWriteType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.bleWriteCallback = bleWriteCallback
            timeOutTask.start(this)
            bleBluetooth.addWriteOperator(key, this)
            mBluetoothGatt!!.writeCharacteristic(characteristic, data, finalWriteType)
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
            if (characteristic.setValue(data)) {
                this.bleWriteCallback = bleWriteCallback
                timeOutTask.start(this)
                bleBluetooth.addWriteOperator(key, this)
                mBluetoothGatt!!.writeCharacteristic(characteristic)
//                if (!mBluetoothGatt!!.writeCharacteristic(mCharacteristic)) {
//                    removeTimeOut()
//                    bleWriteCallback?.onWriteFailure(
//                        BleException.OtherException("gatt writeCharacteristic fail"),
//                        justWrite = data
//                    )
//                }
            } else {
                bleWriteCallback?.onWriteFailure(
                    bleDevice, characteristic,
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
        bleWriteCallback?.onWriteFailure(
            bleDevice,
            gattCharacteristic,
            BleException.TimeoutException(),
            justWrite = data
        )
    }

    override fun destroy() {
        super.destroy()
        data = null
        bleWriteCallback = null
    }
}