package com.huyuhui.fastble.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.huyuhui.fastble.BleManager

@Suppress("unused")
data class BleDevice(
    var scanResult: ScanResult?,
    var device: BluetoothDevice,
) : Parcelable {

    constructor(scanResult: ScanResult) : this(scanResult, scanResult.device)
    constructor(device: BluetoothDevice) : this(null, device)
    //获取名称需要权限，没有权限的时候返回null
    val name: String?
        @SuppressLint("MissingPermission")
        get() {
            return try {
                device.name
            } catch (e: SecurityException) {
                null
            }
        }
    val mac: String
        get() = device.address

    val key
        get() = BleManager.bleFactory?.generateUniqueKey(this) ?: mac

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
     * 获取类型的时候需要权限，没有权限的时候返回null
     * DEVICE_TYPE_UNKNOWN = 0
     * DEVICE_TYPE_CLASSIC = 1
     * DEVICE_TYPE_LE = 2
     * DEVICE_TYPE_DUAL = 3
     */
    val deviceType: Int
        @SuppressLint("MissingPermission")
        get() {
            return try {
                device.type
            } catch (e: SecurityException) {
                0
            }
        }

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
            )!!
        } else {
            parcel.readParcelable(BluetoothDevice::class.java.classLoader)!!
        }
    ) {
        bleAlias = parcel.readString()
        parcel.readMap(propertyMap, HashMap::class.java.classLoader)

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
