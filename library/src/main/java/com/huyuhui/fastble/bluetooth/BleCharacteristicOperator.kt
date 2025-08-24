package com.huyuhui.fastble.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.huyuhui.fastble.data.BleOperatorKey

internal abstract class BleCharacteristicOperator(
    bleBluetooth: BleBluetooth,
    timeout: Long,
    uuidService: String,
    uuidCharacteristic: String
) : BleOperator(bleBluetooth, timeout) {

    val mGattService: BluetoothGattService? =
        fromUUID(uuidService)?.let { mBluetoothGatt?.getService(it) }

    val mCharacteristic: BluetoothGattCharacteristic? = fromUUID(uuidCharacteristic)?.let {
        mGattService?.getCharacteristic(it)
    }

    val key = BleOperatorKey(uuidService, uuidCharacteristic)
}