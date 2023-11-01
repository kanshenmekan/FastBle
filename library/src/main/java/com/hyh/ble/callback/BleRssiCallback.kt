package com.hyh.ble.callback

import com.hyh.ble.exception.BleException

abstract class BleRssiCallback : BleOperateCallback() {
    final override fun onTimeOutFailure(exception: BleException?, data: ByteArray?) {
        onRssiFailure(exception)
    }

    abstract fun onRssiFailure(exception: BleException?)

    abstract fun onRssiSuccess(rssi: Int)
}