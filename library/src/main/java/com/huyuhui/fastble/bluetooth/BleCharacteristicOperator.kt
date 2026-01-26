package com.huyuhui.fastble.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.huyuhui.fastble.data.BleOperatorKey

internal abstract class BleCharacteristicOperator private constructor(
    bleBluetooth: BleBluetooth,
    timeout: Long,
) : BleOperator(bleBluetooth, timeout) {

    var gattService: BluetoothGattService? = null
        private set

    var gattCharacteristic: BluetoothGattCharacteristic? = null
        private set
    lateinit var key: BleOperatorKey

    constructor(
        bleBluetooth: BleBluetooth,
        timeout: Long,
        uuidService: String,
        uuidCharacteristic: String
    ) : this(bleBluetooth, timeout) {
        gattService =
            fromUUID(uuidService)?.let { mBluetoothGatt?.getService(it) }

        gattCharacteristic = fromUUID(uuidCharacteristic)?.let {
            gattService?.getCharacteristic(it)
        }
        key = BleOperatorKey(uuidService, uuidCharacteristic)
    }

    constructor(
        bleBluetooth: BleBluetooth,
        timeout: Long,
        characteristic: BluetoothGattCharacteristic?
    ) : this(bleBluetooth, timeout) {
        gattCharacteristic = characteristic
        gattService = characteristic?.service
        key = BleOperatorKey(gattService?.uuid.toString(), gattCharacteristic?.uuid.toString())
    }

}