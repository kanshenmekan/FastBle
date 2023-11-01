package com.hyh.ble.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Parcel
import android.os.Parcelable

@SuppressLint("MissingPermission")
data class BleDevice(
    var scanResult: ScanResult?,
    var device: BluetoothDevice? = scanResult?.device
) : Parcelable {
    val name
        get() = device?.name
    val mac
        get() = device?.address

    val key
        get() = if (device != null) "$name$mac" else ""

    val scanRecord
        get() = scanResult?.scanRecord?.bytes
    val rssi
        get() = scanResult?.rssi ?: Int.MIN_VALUE

    constructor(parcel: Parcel) : this(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(
                ScanResult::class.java.classLoader,
                ScanResult::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(ScanResult::class.java.classLoader)
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(
                BluetoothDevice::class.java.classLoader,
                BluetoothDevice::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(BluetoothDevice::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(scanResult, flags)
        parcel.writeParcelable(device, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BleDevice> {
        override fun createFromParcel(parcel: Parcel): BleDevice {
            return BleDevice(parcel)
        }

        override fun newArray(size: Int): Array<BleDevice?> {
            return arrayOfNulls(size)
        }
    }


}
