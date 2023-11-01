package com.hyh.ble.callback

import com.hyh.ble.data.BleWriteState
import com.hyh.ble.exception.BleException

abstract class BleWriteCallback : BleOperateCallback() {

    abstract fun onWriteSuccess(
        current: Int = BleWriteState.DATA_WRITE_SINGLE,
        total: Int = BleWriteState.DATA_WRITE_SINGLE,
        justWrite: ByteArray?,
        data: ByteArray? = justWrite
    )

    abstract fun onWriteFailure(
        exception: BleException?,
        current: Int = BleWriteState.DATA_WRITE_SINGLE,
        total: Int = BleWriteState.DATA_WRITE_SINGLE,
        justWrite: ByteArray?,
        data: ByteArray? = justWrite,
        isTotalFail: Boolean = true
    )

    final override fun onTimeOutFailure(exception: BleException?, data: ByteArray?) {
        onWriteFailure(exception, justWrite = data)
    }
}