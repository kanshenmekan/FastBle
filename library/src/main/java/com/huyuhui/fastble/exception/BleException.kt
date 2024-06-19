package com.huyuhui.fastble.exception

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt

sealed class BleException(open val code: Int, open val description: String) {
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
        return "BleException(code=$code, description='$description')"
    }

    data class OtherException(
        override var code: Int = ERROR_CODE_OTHER,
        override var description: String,
    ) :
        BleException(code, description)

    data class TimeoutException(override val description: String = "Timeout Exception Occurred!") :
        BleException(ERROR_CODE_TIMEOUT, description)

    data class DiscoverException(override val description: String = "GATT discover services exception occurred!") :
        BleException(ERROR_CODE_TIMEOUT, description)

    class ConnectException(
        @SuppressLint("PrivateApi") val bluetoothGatt: BluetoothGatt?,
        @SuppressLint("PrivateApi") val gattStatus: Int,
    ) :
        BleException(ERROR_CODE_GATT, "Gatt Exception Occurred! ") {
        override fun toString(): String {
            return "ConnectException(bluetoothGatt=$bluetoothGatt, gattStatus=$gattStatus) ${super.toString()}"
        }
    }

    class GattException(
        @SuppressLint("PrivateApi") val bluetoothGatt: BluetoothGatt?,
        @SuppressLint("PrivateApi") val gattStatus: Int,
    ) :
        BleException(ERROR_CODE_GATT, "Gatt Exception Occurred! ") {
        override fun toString(): String {
            return "GattException(bluetoothGatt=$bluetoothGatt, gattStatus=$gattStatus) ${super.toString()}"
        }
    }
}