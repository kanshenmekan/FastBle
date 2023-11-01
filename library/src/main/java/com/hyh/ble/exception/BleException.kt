package com.hyh.ble.exception

import android.bluetooth.BluetoothGatt

sealed class BleException(var code: Int, open var description: String) {
    companion object {
        @JvmStatic
        val ERROR_CODE_TIMEOUT = 100

        @JvmStatic
        val ERROR_CODE_GATT = 101

        @JvmStatic
        val ERROR_CODE_OTHER = 102
    }

    override fun toString(): String {
        return "BleException(code=$code, description='$description')"
    }

    data class OtherException(override var description: String) :
        BleException(ERROR_CODE_OTHER, description)

    data class TimeoutException(override var description: String = "Timeout Exception Occurred!") :
        BleException(ERROR_CODE_TIMEOUT, description)

    data class DiscoverException(override var description: String = "GATT discover services exception occurred!") :
        BleException(ERROR_CODE_TIMEOUT, description)

    class ConnectException(var bluetoothGatt: BluetoothGatt?, var gattStatus: Int) :
        BleException(ERROR_CODE_GATT, "Gatt Exception Occurred! ") {
        override fun toString(): String {
            return "ConnectException(bluetoothGatt=$bluetoothGatt, gattStatus=$gattStatus) ${super.toString()}"
        }
    }

    class GattException(var bluetoothGatt: BluetoothGatt?, var gattStatus: Int) :
        BleException(ERROR_CODE_GATT, "Gatt Exception Occurred! ") {
        override fun toString(): String {
            return "GattException(bluetoothGatt=$bluetoothGatt, gattStatus=$gattStatus) ${super.toString()}"
        }
    }
}