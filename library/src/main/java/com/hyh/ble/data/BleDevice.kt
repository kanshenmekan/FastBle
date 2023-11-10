package com.hyh.ble.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.hyh.ble.BleManager


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
    val isConnected
        get() = BleManager.isConnected(this)
    val isConnecting
        get() = BleManager.isConnecting(this)

    var bleAlias: String? = null

    /**
     * DEVICE_TYPE_UNKNOWN = 0
     * DEVICE_TYPE_CLASSIC = 1
     * DEVICE_TYPE_LE = 2
     * DEVICE_TYPE_DUAL = 3
     */
    val deviceType
        get() = device?.type?:BluetoothDevice.DEVICE_TYPE_UNKNOWN

    /**
     * 自定义属性值
     */
    private val propertyMap: HashMap<String, Any> by lazy {
        hashMapOf()
    }
    @Suppress("DEPRECATION")
    constructor(parcel: Parcel) : this(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(
                ScanResult::class.java.classLoader,
                ScanResult::class.java
            )
        } else {
            parcel.readParcelable(ScanResult::class.java.classLoader)
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(
                BluetoothDevice::class.java.classLoader,
                BluetoothDevice::class.java
            )
        } else {
            parcel.readParcelable(BluetoothDevice::class.java.classLoader)
        }
    ) {
        bleAlias = parcel.readString()
        parcel.readMap(propertyMap,HashMap::class.java.classLoader)

    }

    fun put(key: String, value: Any) {
        propertyMap[key] = value
    }
    operator fun get(key: String?): Any? {
        return propertyMap[key]
    }
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(scanResult, flags)
        parcel.writeParcelable(device, flags)
        parcel.writeString(bleAlias)
        parcel.writeMap(propertyMap)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "BleDevice(scanResult=$scanResult, device=$device, bleAlias=$bleAlias, deviceType=$deviceType, propertyMap=$propertyMap)"
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
