package com.hyh.ble.callback

import com.hyh.ble.exception.BleException

abstract class BleMtuChangedCallback : BleOperateCallback() {

    abstract fun onSetMTUFailure(exception: BleException?)

    abstract fun onMtuChanged(mtu: Int)

    final override fun onTimeOutFailure(exception: BleException?, data: ByteArray?) {
        onSetMTUFailure(exception)
    }
}