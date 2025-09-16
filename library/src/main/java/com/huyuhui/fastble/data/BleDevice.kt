package com.huyuhui.fastble.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.utils.BleLog

@Suppress("unused")
data class BleDevice(
    val device: BluetoothDevice
) : Parcelable {
    var scanResult: ScanResult? = null

    constructor(scanResult: ScanResult) : this(scanResult.device) {
        this.scanResult = scanResult
    }

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

    val key = BleManager.bleFactory?.generateUniqueKey(this) ?: mac

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
     * 自定义属性值：仅支持以下类型，其他类型会导致序列化失败
     * - 基础类型：String、Int、Long、Float、Double、Boolean、ByteArray
     * - 自定义类型：实现 Parcelable 接口的类
     */
    private val propertyMap: HashMap<String, Any> by lazy {
        hashMapOf()
    }

    @Suppress("DEPRECATION")
    constructor(parcel: Parcel) : this(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(
                BluetoothDevice::class.java.classLoader,
                BluetoothDevice::class.java
            )!!
        } else {
            parcel.readParcelable(BluetoothDevice::class.java.classLoader)!!
        }
    ) {
        scanResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(
                ScanResult::class.java.classLoader,
                ScanResult::class.java
            )
        } else {
            parcel.readParcelable(ScanResult::class.java.classLoader)
        }
        bleAlias = parcel.readString()
        parcel.readMap(propertyMap, HashMap::class.java.classLoader)

    }

    /**
     * 存入自定义属性（仅支持指定合法类型，非法类型会打警告并拒绝存入）
     * @param key 属性键
     * @param value 属性值：支持基础类型（String/Int/Long等）或 Parcelable 类型
     */
    fun put(key: String, value: Any) {
        val isLegalType = when (value) {
            is String, is Int, is Long, is Float, is Double, is Boolean, is ByteArray -> true
            is Parcelable -> true
            else -> false
        }
        if (!isLegalType) {
            BleLog.e("put failed: Only basic types (String/Int/Long/Boolean/ByteArray) or Parcelable are supported! Illegal type: ${value.javaClass.name}")
            return
        }
        propertyMap[key] = value
    }

    operator fun get(key: String?): Any? {
        return propertyMap[key]
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(device, flags)
        parcel.writeParcelable(scanResult, flags)
        parcel.writeString(bleAlias)
        try {
            parcel.writeMap(propertyMap)
        } catch (e: Exception) {
            BleLog.e("Only basic data types or Parcelable can be written.", e)
            parcel.writeMap(hashMapOf<String, Any>()) // 写入空 Map 避免后续反序列化异常
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "BleDevice(scanResult=$scanResult, device=$device, bleAlias=$bleAlias, deviceType=$deviceType, propertyMap=$propertyMap)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleDevice

        return key == other.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
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
