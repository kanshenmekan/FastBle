package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import com.huyuhui.fastble.callback.BleIndicateCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.exception.BleException
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
internal class BleIndicateOperator(bleBluetooth: BleBluetooth) : BleOperator(bleBluetooth) {

    var bleIndicateCallback: BleIndicateCallback? = null
        private set

    fun enableCharacteristicIndicate(
        bleIndicateCallback: BleIndicateCallback?,
        uuidIndicate: String,
        useCharacteristicDescriptor: Boolean,
    ) {
        if (mCharacteristic != null
            && mCharacteristic!!.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0
        ) {
            this.bleIndicateCallback = bleIndicateCallback
            bleBluetooth.addIndicateOperator(uuidIndicate, this)
            launch {
                timeOutTask.start()
            }
            setCharacteristicIndication(
                mBluetoothGatt, mCharacteristic,
                true, bleIndicateCallback, useCharacteristicDescriptor
            )
        } else {
            bleIndicateCallback?.onIndicateFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_NOT_SUPPORT,
                    "this characteristic not support indicate!"
                )
            )
        }
    }


    /**
     * stop indicate
     */
    fun disableCharacteristicIndicate(useCharacteristicDescriptor: Boolean): Boolean {
        return if (mCharacteristic != null
            && mCharacteristic!!.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0
        ) {
            setCharacteristicIndication(
                mBluetoothGatt, mCharacteristic,
                false, null, useCharacteristicDescriptor
            )
        } else {
            false
        }
    }

    /**
     * indicate setting
     */
    @Suppress("DEPRECATION")
    private fun setCharacteristicIndication(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        enable: Boolean,
        bleIndicateCallback: BleIndicateCallback?,
        useCharacteristicDescriptor: Boolean,
    ): Boolean {
        if (gatt == null || characteristic == null) {
            removeTimeOut()
            bleIndicateCallback?.onIndicateFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(
                    BleException.GATT_NULL,
                    "gatt or characteristic equal null"
                )
            )
            return false
        }
        val success1 = gatt.setCharacteristicNotification(characteristic, enable)
        if (!success1) {
            removeTimeOut()
            bleIndicateCallback?.onIndicateFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_ERROR,
                    "gatt setCharacteristicNotification fail"
                )
            )
            return false
        }
        val descriptor: BluetoothGattDescriptor? =
            if (useCharacteristicDescriptor) characteristic.getDescriptor(characteristic.uuid) else
                characteristic.getDescriptor(fromUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR))
        return if (descriptor == null) {
            removeTimeOut()
            bleIndicateCallback?.onIndicateFailure(
                bleDevice,
                mCharacteristic,
                BleException.OtherException(BleException.DESCRIPTOR_NULL, "descriptor equals null")
            )
            false
        } else {
            val data =
                if (enable) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            descriptor.value = data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val state = gatt.writeDescriptor(
                    descriptor,
                    data!!
                )
                val success2 = state == BluetoothStatusCodes.SUCCESS
                if (!success2) {
                    removeTimeOut()
                    bleIndicateCallback?.onIndicateFailure(
                        bleDevice,
                        mCharacteristic,
                        BleException.OtherException(
                            BleException.DESCRIPTOR_ERROR,
                            "gatt writeDescriptor fail"
                        )
                    )
                }
                success2
            } else {
                val success2 = gatt.writeDescriptor(descriptor)
                if (!success2) {
                    removeTimeOut()
                    bleIndicateCallback?.onIndicateFailure(
                        bleDevice,
                        mCharacteristic,
                        BleException.OtherException(
                            BleException.DESCRIPTOR_ERROR,
                            "gatt writeDescriptor fail"
                        )
                    )
                }
                success2
            }
        }
    }

    override fun onTimeout(
        task: TimeoutTask,
        e: Throwable?,
        isActive: Boolean
    ) {
        bleIndicateCallback?.onIndicateFailure(
            bleDevice,
            mCharacteristic,
            BleException.TimeoutException()
        )
    }

    override fun destroy() {
        super.destroy()
        bleIndicateCallback = null
    }
}