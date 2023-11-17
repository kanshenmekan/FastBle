package com.hyh.ble.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.hyh.ble.BleManager
import com.hyh.ble.callback.BleScanCallback
import com.hyh.ble.data.BleDevice
import com.hyh.ble.data.BleScanState
import com.hyh.ble.utils.BleLog
import com.hyh.ble.utils.HexUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
internal object BleScanner : ScanCallback(), CoroutineScope by MainScope() {
    var mBleScanState = BleScanState.STATE_IDLE
        private set
    private val bleScanRuleConfig
        get() = BleManager.bleScanRuleConfig
    var bleScanCallback: BleScanCallback? = null
    private val map = linkedMapOf<String, BleDevice>()
    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        mBleScanState = BleScanState.STATE_IDLE
        bleScanCallback?.onScanStarted(false)
    }

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        super.onScanResult(callbackType, result)
        if (result == null) return
        val bleDevice = BleDevice(result)
        if (bleScanRuleConfig.mFuzzyName && !bleScanRuleConfig.mDeviceNames.isNullOrEmpty()) {
            if (bleDevice.name == null) return
            var hasFound = false
            bleScanRuleConfig.mDeviceNames?.forEach {
                if (bleDevice.name!!.contains(it, true)) {
                    hasFound = true
                }
            }
            if (!hasFound) return
        }
        if (bleScanCallback?.onFilter(bleDevice) != false) {
            correctDeviceAndNextStep(bleDevice)
        }
    }

    private val mutex = Mutex()
    private fun correctDeviceAndNextStep(bleDevice: BleDevice) {
        launch(Dispatchers.IO) {
            mutex.withLock {
                if (!map.contains(bleDevice.key)) {
                    BleLog.i(
                        "device detected  ------  name: ${bleDevice.name}  mac: ${bleDevice.mac} " +
                                "  Rssi: ${bleDevice.rssi}  scanRecord: ${
                                    HexUtil.formatHexString(
                                        bleDevice.scanRecord,
                                        true
                                    )
                                }"
                    )
                    map[bleDevice.key] = bleDevice
                    withContext(Dispatchers.Main) {
                        bleScanCallback?.onLeScan(bleDevice, bleDevice, false)
                    }
                } else {
                    val oldDevice = map[bleDevice.key]
                    map[bleDevice.key] = bleDevice
                    withContext(Dispatchers.Main) {
                        bleScanCallback?.onLeScan(oldDevice!!, bleDevice, true)
                    }
                }
            }
        }
    }

    @Synchronized
    fun startLeScan() {
        if (mBleScanState != BleScanState.STATE_IDLE) {
            BleLog.w("scan action already exists, complete the previous scan action first")
            bleScanCallback?.onScanStarted(false)
            return
        }
        map.clear()
        bleScanCallback?.onScanStarted(true)
        mBleScanState = BleScanState.STATE_SCANNING
        launch {
            BleManager.bluetoothAdapter?.bluetoothLeScanner?.startScan(
                bleScanRuleConfig.generateScanFilter(),
                bleScanRuleConfig.generateScanSettings(), this@BleScanner
            )
            if (bleScanRuleConfig.mScanTimeOut > 0) {
                delay(bleScanRuleConfig.mScanTimeOut)
                stopLeScan()
            }
        }

    }

    @Synchronized
    fun stopLeScan() {
        if (mBleScanState == BleScanState.STATE_SCANNING) {
            BleManager.bluetoothAdapter?.bluetoothLeScanner?.stopScan(this@BleScanner)
            mBleScanState = BleScanState.STATE_IDLE
            bleScanCallback?.onScanFinished(map.values.toList())
            coroutineContext.cancelChildren()
        }
    }

    fun destroy() {
        bleScanCallback = null
        map.clear()
        stopLeScan()
    }
}