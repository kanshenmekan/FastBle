package com.hyh.ble.common

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.hyh.ble.BleManager
import com.hyh.ble.scan.BleScanner
import java.lang.ref.WeakReference


class BluetoothChangedObserver {
    private var mBleReceiver: BleReceiver? = null
    var bleStatusCallback:BleStatusCallback? = null
    interface BleStatusCallback {
        fun onStateOn()
        fun onStateTurningOn()
        fun onStateOff()
        fun onStateTurningOff()
    }

    fun registerReceiver(context: Context) {
        mBleReceiver = BleReceiver(this)
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(mBleReceiver, filter)
    }
    fun unregisterReceiver(context: Context) {
        if (mBleReceiver != null){
            try {
                context.unregisterReceiver(mBleReceiver)
                bleStatusCallback = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    class BleReceiver(bluetoothChangedObserver: BluetoothChangedObserver) : BroadcastReceiver() {
        private var mObserverWeakReference: WeakReference<BluetoothChangedObserver>

        init {
            mObserverWeakReference = WeakReference(bluetoothChangedObserver)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED){
                val observer = mObserverWeakReference.get()
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        observer?.bleStatusCallback?.onStateOff()
                        BleScanner.stopLeScan()
                        BleManager.multipleBluetoothController.onBleOff()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF ->{
                        observer?.bleStatusCallback?.onStateTurningOff()
                    }

                    BluetoothAdapter.STATE_ON -> {
                        observer?.bleStatusCallback?.onStateOn()
                    }

                    BluetoothAdapter.STATE_TURNING_ON -> {
                        observer?.bleStatusCallback?.onStateTurningOn()
                    }
                }
            }
        }
    }
}