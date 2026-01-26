package com.huyuhui.fastble.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import com.huyuhui.fastble.callback.BleNotifyCallback
import com.huyuhui.fastble.common.TimeoutTask
import com.huyuhui.fastble.exception.BleException

@SuppressLint("MissingPermission")
internal class BleNotifyOperator(
    bleBluetooth: BleBluetooth,
    timeout: Long,
    uuidService: String,
    uuidCharacteristic: String
) : BleCharacteristicOperator(bleBluetooth, timeout, uuidService, uuidCharacteristic) {
    var bleNotifyCallback: BleNotifyCallback? = null
        private set

    /**
     * notify
     */
    fun enableCharacteristicNotify(
        bleNotifyCallback: BleNotifyCallback?,
        useCharacteristicDescriptor: Boolean,
    ) {
        val characteristic = gattCharacteristic
        if (characteristic == null) {
            bleNotifyCallback?.onNotifyFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_ERROR,
                    "characteristic is null!"
                )
            )
            return
        }
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            bleNotifyCallback?.onNotifyFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(
                    BleException.CHARACTERISTIC_NOT_SUPPORT,
                    "this characteristic not support notify!"
                )
            )
            return
        }
        this@BleNotifyOperator.bleNotifyCallback = bleNotifyCallback
        bleBluetooth.addNotifyOperator(key, this)
        timeOutTask.start(this)
        setCharacteristicNotification(
            mBluetoothGatt,
            characteristic,
            true,
            bleNotifyCallback,
            useCharacteristicDescriptor
        )
    }

    /**
     * stop notify
     */
    fun disableCharacteristicNotify(useCharacteristicDescriptor: Boolean): Boolean {
        val characteristic = gattCharacteristic
        return if (characteristic != null
            && characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        ) {
            setCharacteristicNotification(
                mBluetoothGatt, characteristic, false, null, useCharacteristicDescriptor
            )
        } else {
            false
        }
    }


    @Suppress("DEPRECATION")
    private fun setCharacteristicNotification(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        enable: Boolean,
        bleNotifyCallback: BleNotifyCallback?,
        useCharacteristicDescriptor: Boolean,
    ): Boolean {
        if (gatt == null || characteristic == null) {
            removeTimeOut()
            bleNotifyCallback?.onNotifyFailure(
                bleDevice,
                characteristic,
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
            bleNotifyCallback?.onNotifyFailure(
                bleDevice,
                characteristic,
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
            bleNotifyCallback?.onNotifyFailure(
                bleDevice,
                characteristic,
                BleException.OtherException(BleException.DESCRIPTOR_NULL, "descriptor equals null")
            )
            false
        } else {
            val data =
                if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            descriptor.value = data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val state = gatt.writeDescriptor(
                    descriptor,
                    data
                )
                val success2 = state == BluetoothStatusCodes.SUCCESS
                if (!success2) {
                    removeTimeOut()
                    bleNotifyCallback?.onNotifyFailure(
                        bleDevice,
                        characteristic,
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
                    bleNotifyCallback?.onNotifyFailure(
                        bleDevice,
                        characteristic,
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
        bleNotifyCallback?.onNotifyFailure(
            bleDevice,
            gattCharacteristic,
            BleException.TimeoutException()
        )
    }

    override fun destroy() {
        super.destroy()
        bleNotifyCallback = null
    }
}