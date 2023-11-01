package com.hyh.ble.callback

import com.hyh.ble.exception.BleException

abstract class BleNotifyCallback : BleOperateCallback() {
    abstract fun onNotifySuccess()

    abstract fun onNotifyFailure(exception: BleException?)

    abstract fun onCharacteristicChanged(data: ByteArray?)

    final override fun onTimeOutFailure(exception: BleException?,data: ByteArray?) {
        onNotifyFailure(exception)
    }
}