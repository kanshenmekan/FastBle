package com.huyuhui.blesample

import android.app.Application
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.common.BleConnectStrategy

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        BleManager.apply {
            enableLog(true)
            maxConnectCount = 5
            operateTimeout = 2000
            splitWriteNum = 20
            bleConnectStrategy = BleConnectStrategy().apply {
                connectOverTime = 10000
                connectBackpressureStrategy = BleConnectStrategy.CONNECT_BACKPRESSURE_DROP
                setReConnectCount(1,2000)
            }
        }.init(this)
    }
}