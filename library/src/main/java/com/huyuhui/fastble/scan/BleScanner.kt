package com.huyuhui.fastble.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.callback.BleScanCallback
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.data.BleScanState
import com.huyuhui.fastble.exception.BleMainScope
import com.huyuhui.fastble.utils.BleLog
import com.huyuhui.fastble.utils.HexUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
internal object BleScanner : ScanCallback(), CoroutineScope by BleMainScope({ _, throwable ->
    BleLog.e("BleScanner: a coroutine error has occurred ${throwable.message}")
}) {
    var mBleScanState = BleScanState.STATE_IDLE
        private set
    private val bleScanRuleConfig
        get() = BleManager.bleScanRuleConfig
    var bleScanCallback: BleScanCallback? = null
    private val map = linkedMapOf<String, BleDevice>()
    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        BleLog.e("scan failed,errorCode = $errorCode")
        mBleScanState = BleScanState.STATE_IDLE
        bleScanCallback?.onScanStarted(false)
    }

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        super.onScanResult(callbackType, result)
        launch(Dispatchers.IO) {
            if (result == null) return@launch
            val bleDevice = BleDevice(result)
            if (bleScanRuleConfig.mFuzzyName && !bleScanRuleConfig.mDeviceNames.isNullOrEmpty()) {
                if (bleDevice.name == null) return@launch
                var hasFound = false
                bleScanRuleConfig.mDeviceNames?.forEach forEach@{
                    if (bleDevice.name!!.contains(it, true)) {
                        hasFound = true
                        return@forEach
                    }
                }
                if (!hasFound) return@launch
            }
            if (bleScanCallback?.onFilter(bleDevice) != false) {
                correctDeviceAndNextStep(bleDevice)
            }
        }
    }

    private val mutex = Mutex()
    private suspend fun correctDeviceAndNextStep(bleDevice: BleDevice) {
        mutex.withLock {
            ensureActive()
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

    @Synchronized
    fun startLeScan(scanTimeout: Long) {
        if (mBleScanState != BleScanState.STATE_IDLE) {
            BleLog.w("scan action already exists, complete the previous scan action first")
            bleScanCallback?.onScanStarted(false)
            return
        }
        map.clear()
        bleScanCallback?.onScanStarted(true)
        BleLog.i("scan start")
        mBleScanState = BleScanState.STATE_SCANNING
        launch {
            BleManager.bluetoothAdapter?.bluetoothLeScanner?.startScan(
                bleScanRuleConfig.generateScanFilter(),
                bleScanRuleConfig.generateScanSettings(), this@BleScanner
            )
            if (scanTimeout > 0) {
                delay(scanTimeout)
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
            BleLog.i("scan finished")
            coroutineContext.cancelChildren()
        }
    }

    fun destroy() {
        bleScanCallback = null
        map.clear()
        stopLeScan()
    }
}