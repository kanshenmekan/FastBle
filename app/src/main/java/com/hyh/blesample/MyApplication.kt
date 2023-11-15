package com.hyh.blesample

import android.app.Application
import com.hyh.ble.BleManager
import com.hyh.ble.common.BleConnectStrategy

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        BleManager.apply {
            bleConnectStrategy = BleConnectStrategy().apply {
                maxConnectCount = 1
                connectOverTime = 12000
                setReConnectCount(1,2000)
            }
        }.init(this)
    }
}