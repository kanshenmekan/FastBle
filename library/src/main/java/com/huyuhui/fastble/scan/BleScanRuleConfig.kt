package com.huyuhui.fastble.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.huyuhui.fastble.BleManager
import java.util.UUID

@Suppress("unused")
class BleScanRuleConfig private constructor() {

    var mServiceUuids: List<UUID>? = null
        private set
    var mDeviceMacs: List<String>? = null
        private set
    var mDeviceNames: List<String>? = null
        private set
    var mScanTimeOut = BleManager.DEFAULT_SCAN_TIME
        private set
    var mFuzzyName: Boolean = false
        private set

    private var scanSettings: ScanSettings? = null

    fun generateScanFilter(): List<ScanFilter> {
        val scanFilters = mutableListOf<ScanFilter>()
        mServiceUuids?.forEach {
            val build = ScanFilter.Builder().setServiceUuid(ParcelUuid(it))
            scanFilters.add(build.build())
        }
        if (!mFuzzyName) {
            mDeviceNames?.forEach {
                val build = ScanFilter.Builder().setDeviceName(it)
                scanFilters.add(build.build())
            }
        }
        mDeviceMacs?.forEach {
            val build = ScanFilter.Builder().setDeviceAddress(it)
            scanFilters.add(build.build())
        }
        return scanFilters
    }

    /**
     *     public static final int CALLBACK_TYPE_ALL_MATCHES = 1; //寻找符合过滤条件的蓝牙广播，如果没有设置过滤条件，则返回全部广播包
    public static final int CALLBACK_TYPE_FIRST_MATCH = 2; //要设置过滤条件，不设置就不返回，首次匹配的设备的时候才会回调。
    public static final int CALLBACK_TYPE_MATCH_LOST = 4; // 要设置过滤条件，之前搜索过滤完符合条件，后面搜索的时候，没有找到了，当设备不再匹配过滤条件时，您可以触发警报或采取其他适当的措施。
    @NonNull
    public static final int MATCH_MODE_AGGRESSIVE = 1;  //激进模式，即使信号强度微弱且持续时间内瞄准/匹配的次数很少，hw也会更快地确定匹配
    public static final int MATCH_MODE_STICKY = 2; //粘性模式，在通过硬件报告之前，需要更高的信号强度和目击阈值
    public static final int MATCH_NUM_FEW_ADVERTISEMENT = 2; //每个过滤器过滤少量的设备，取决于系统资源
    public static final int MATCH_NUM_MAX_ADVERTISEMENT = 3; //每个过滤器尽可能匹配更多的广播，取决于系统资源
    public static final int MATCH_NUM_ONE_ADVERTISEMENT = 1; //确定的数量，因为系统资源少
    public static final int PHY_LE_ALL_SUPPORTED = 255;
    public static final int SCAN_MODE_BALANCED = 1; //平衡模式
    public static final int SCAN_MODE_LOW_LATENCY = 2; //低延时扫描,高功耗模式(建议仅在应用程序在前台运行时才使用此模式。)
    public static final int SCAN_MODE_LOW_POWER = 0; //低功耗模式(默认扫描模式,如果扫描应用程序不在前台，则强制使用此模式。)
    public static final int SCAN_MODE_OPPORTUNISTIC = -1; //空闲时扫描
     */
    fun generateScanSettings(): ScanSettings {
        return if (scanSettings == null) {
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
        } else {
            scanSettings!!
        }
    }

    class Builder {
        private var mServiceUuids: List<UUID>? = null
        private var mDeviceNames: List<String>? = null
        private var mDeviceMacs: List<String>? = null
        private var mTimeOut = BleManager.DEFAULT_SCAN_TIME
        private var scanSettings: ScanSettings? = null
        private var mFuzzyName: Boolean = false
        fun setServiceUuids(uuids: List<UUID>?): Builder {
            mServiceUuids = uuids
            return this
        }

        fun setDeviceName(vararg names: String, isFuzzy: Boolean = false): Builder {
            return setDeviceName(names.toList(), isFuzzy)
        }

        fun setDeviceName(names: List<String>?, isFuzzy: Boolean = false): Builder {
            mDeviceNames = names
            mFuzzyName = isFuzzy
            return this
        }

        fun setDeviceMac(vararg macs: String): Builder {
            mDeviceMacs = macs.toList()
            return this
        }


        fun setScanTimeOut(timeOut: Long): Builder {
            mTimeOut = timeOut
            return this
        }

        fun setScanSettings(scanSettings: ScanSettings) {
            this.scanSettings = scanSettings
        }
        @SuppressLint("PrivateApi")
        fun applyConfig(config: BleScanRuleConfig) {
            config.mServiceUuids = mServiceUuids
            config.mDeviceNames = mDeviceNames
            config.mDeviceMacs = mDeviceMacs
            config.mScanTimeOut = mTimeOut
            config.scanSettings = scanSettings
            config.mFuzzyName = mFuzzyName
        }

        fun build(): BleScanRuleConfig {
            val config = BleScanRuleConfig()
            applyConfig(config)
            return config
        }
    }

}