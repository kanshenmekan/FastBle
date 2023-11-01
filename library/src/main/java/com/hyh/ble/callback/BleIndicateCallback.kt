package com.hyh.ble.callback

import com.hyh.ble.exception.BleException

abstract class BleIndicateCallback :BleOperateCallback(){
    abstract fun onIndicateSuccess()

    abstract fun onIndicateFailure(exception: BleException?)

    abstract fun onCharacteristicChanged(data: ByteArray?)

    final override fun onTimeOutFailure(exception: BleException?,data: ByteArray?) {
        onIndicateFailure(exception)
    }
}