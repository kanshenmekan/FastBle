package com.hyh.ble.callback

import com.hyh.ble.data.BleDevice

interface BleScanCallback {
    fun onScanStarted(success: Boolean)
    fun onLeScan(oldDevice: BleDevice?,newDevice:BleDevice?,scannedBefore:Boolean)
    fun onScanFinished(scanResultList: List<BleDevice>)
    fun onFilter(bleDevice: BleDevice):Boolean
}