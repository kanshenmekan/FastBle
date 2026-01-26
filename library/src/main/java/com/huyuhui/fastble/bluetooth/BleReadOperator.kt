package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.callback.BleReadCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.exception.BleException

@SuppressLint("MissingPermission")
internal class BleReadOperator : BleCharacteristicOperator {

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

    var bleReadCallback: BleReadCallback? = null
        private set

    /**
     * read
     */
    fun readCharacteristic(bleReadCallback: BleReadCallback?) {
        val characteristic = gattCharacteristic
        if (characteristic == null) {
            bleReadCallback?.onReadFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_ERROR,
                    "characteristic is null!"
                )
            )
            return
        }
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            bleReadCallback?.onReadFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_NOT_SUPPORT,
                    "this characteristic not support read!"
                )
            )
            return
        }

        this.bleReadCallback = bleReadCallback
        timeOutTask.start(this)
        bleBluetooth.addReadOperator(key, this)
        if (!mBluetoothGatt!!.readCharacteristic(characteristic)) {
            removeTimeOut()
            bleReadCallback?.onReadFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_ERROR,
                    "gatt readCharacteristic fail"
                )
            )
        }
    }

    override fun onTimeout(
        task: TimeoutTask,
        e: Throwable?,
        isActive: Boolean
    ) {
        bleReadCallback?.onReadFailure(
            bleDevice,
            gattCharacteristic,
            BleException.TimeoutException()
        )
    }

    override fun destroy() {
        super.destroy()
        bleReadCallback = null
    }
}