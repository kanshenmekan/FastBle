package com.huyuhui.fastble.callback

import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.data.BleWriteState
import com.huyuhui.fastble.exception.BleException

abstract class BleWriteCallback : BleOperateCallback() {

    abstract fun onWriteSuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        current: Int = BleWriteState.DATA_WRITE_SINGLE,
        total: Int = BleWriteState.DATA_WRITE_SINGLE,
        justWrite: ByteArray,
        data: ByteArray = justWrite
    )

    abstract fun onWriteFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        current: Int = BleWriteState.DATA_WRITE_SINGLE,
        total: Int = BleWriteState.DATA_WRITE_SINGLE,
        justWrite: ByteArray?,
        data: ByteArray? = justWrite,
        isTotalFail: Boolean = true
    )

}