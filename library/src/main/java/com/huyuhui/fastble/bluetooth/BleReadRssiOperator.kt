package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import com.huyuhui.fastble.callback.BleRssiCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.exception.BleException

@SuppressLint("MissingPermission")
internal class BleReadRssiOperator(
    bleBluetooth: BleBluetooth,
    timeout: Long
) : BleOperator(bleBluetooth, timeout) {
    var bleRssiCallback: BleRssiCallback? = null
        private set

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
            timeOutTask.start(this)
            this.bleRssiCallback = bleRssiCallback
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

    override fun onTimeout(
        task: TimeoutTask,
        e: Throwable?,
        isActive: Boolean
    ) {
        bleRssiCallback?.onRssiFailure(bleDevice, BleException.TimeoutException())
    }

    override fun destroy() {
        super.destroy()
        bleRssiCallback = null
    }
}
