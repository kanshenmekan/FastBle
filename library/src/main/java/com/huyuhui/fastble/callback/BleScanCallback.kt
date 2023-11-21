package com.huyuhui.fastble.callback

import com.huyuhui.fastble.data.BleDevice

interface BleScanCallback {
    fun onScanStarted(success: Boolean)
    fun onLeScan(oldDevice: BleDevice, newDevice: BleDevice, scannedBefore: Boolean)
    fun onScanFinished(scanResultList: List<BleDevice>)
    fun onFilter(bleDevice: BleDevice): Boolean
}