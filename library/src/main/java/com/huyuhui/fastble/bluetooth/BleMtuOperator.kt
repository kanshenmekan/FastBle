package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import com.huyuhui.fastble.callback.BleMtuChangedCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.exception.BleException
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
internal class BleMtuOperator(bleBluetooth: BleBluetooth) : BleOperator(bleBluetooth) {

    var bleMtuChangedCallback: BleMtuChangedCallback? = null
        private set

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
            this.bleMtuChangedCallback = bleMtuChangedCallback
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

    override fun onTimeout(
        task: TimeoutTask,
        e: Throwable?,
        isActive: Boolean
    ) {
        bleMtuChangedCallback?.onSetMTUFailure(bleDevice, BleException.TimeoutException())
    }

    override fun destroy() {
        super.destroy()
        bleMtuChangedCallback = null
    }
}