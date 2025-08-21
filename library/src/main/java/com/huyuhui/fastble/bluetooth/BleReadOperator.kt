package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.callback.BleReadCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.exception.BleException
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
internal class BleReadOperator(bleBluetooth: BleBluetooth) : BleOperator(bleBluetooth) {
    var bleReadCallback: BleReadCallback? = null
        private set

    /**
     * read
     */
    fun readCharacteristic(bleReadCallback: BleReadCallback?, uuidRead: String) {
        if (mCharacteristic != null
            && mCharacteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_READ > 0
        ) {
            this.bleReadCallback = bleReadCallback
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

    override fun onTimeout(
        task: TimeoutTask,
        e: Throwable?,
        isActive: Boolean
    ) {
        bleReadCallback?.onReadFailure(bleDevice,mCharacteristic, BleException.TimeoutException())
    }

    override fun destroy() {
        super.destroy()
        bleReadCallback = null
    }
}