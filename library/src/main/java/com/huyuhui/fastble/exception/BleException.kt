package com.huyuhui.fastble.exception

import android.bluetooth.BluetoothGatt

sealed class BleException(open val code: Int, message: String) : Throwable(message) {
    companion object {
        @JvmStatic
        val ERROR_CODE_TIMEOUT = 100

        @JvmStatic
        val ERROR_CODE_GATT = 101

        @JvmStatic
        val ERROR_CODE_OTHER = 102

        @JvmStatic
        val NOT_SUPPORT_BLE = 2005

        @JvmStatic
        val BLUETOOTH_NOT_ENABLED = 2006

        @JvmStatic
        val DEVICE_NULL = 2007

        @JvmStatic
        val DEVICE_NOT_CONNECT = 2008

        @JvmStatic
        val DATA_NULL = 2009

        @JvmStatic
        val GATT_NULL = 2010

        @JvmStatic
        val CHARACTERISTIC_NOT_SUPPORT = 2011

        @JvmStatic
        val CHARACTERISTIC_ERROR = 2012

        @JvmStatic
        val DESCRIPTOR_NULL = 2013

        @JvmStatic
        val DESCRIPTOR_ERROR = 2014

        @JvmStatic
        val COROUTINE_SCOPE_CANCELLED = 2015

//        @JvmStatic
//        val DEVICE_HAS_CONNECTED = 2016
    }

    override fun toString(): String {
        return "BleException(code=$code, message='$message')"
    }

    class OtherException(
        override val code: Int = ERROR_CODE_OTHER,
        message: String,
    ) : BleException(code, message) {
        override fun toString(): String {
            return "OtherException(code=$code, message='$message')"
        }
    }

    class TimeoutException(message: String = "Timeout Exception Occurred!") :
        BleException(ERROR_CODE_TIMEOUT, message) {
        override fun toString(): String {
            return "TimeoutException(code=$code, message='$message')"
        }
    }

    class DiscoverException(
        override val code: Int = ERROR_CODE_GATT,
        message: String = "GATT discover services exception occurred!"
    ) : BleException(code, message) {
        override fun toString(): String {
            return "DiscoverException(code=$code, message='$message')"
        }
    }

    class ConnectException(
        val bluetoothGatt: BluetoothGatt?,
        val gattStatus: Int,
    ) : BleException(ERROR_CODE_GATT, "Gatt Exception Occurred!") {
        override fun toString(): String {
            return "ConnectException(bluetoothGatt=$bluetoothGatt, gattStatus=$gattStatus) code=$code, message='$message'"
        }
    }

    class GattException(
        val bluetoothGatt: BluetoothGatt?,
        val gattStatus: Int,
    ) : BleException(ERROR_CODE_GATT, "Gatt Exception Occurred!") {
        override fun toString(): String {
            return "GattException(bluetoothGatt=$bluetoothGatt, gattStatus=$gattStatus) code=$code, message='$message'"
        }
    }
}