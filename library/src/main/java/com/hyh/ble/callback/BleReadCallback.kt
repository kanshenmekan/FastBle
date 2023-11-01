package com.hyh.ble.callback

import com.hyh.ble.exception.BleException

abstract class BleReadCallback : BleOperateCallback() {
    final override fun onTimeOutFailure(exception: BleException?, data: ByteArray?) {
        onReadFailure(exception)
    }

    abstract fun onReadSuccess(data: ByteArray?)

    abstract fun onReadFailure(exception: BleException?)
}