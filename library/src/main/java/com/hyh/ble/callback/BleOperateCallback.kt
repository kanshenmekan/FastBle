package com.hyh.ble.callback

import com.hyh.ble.exception.BleException

abstract class BleOperateCallback {
    abstract fun onTimeOutFailure(exception: BleException?,data: ByteArray?)
}