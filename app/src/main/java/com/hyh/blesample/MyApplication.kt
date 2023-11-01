package com.hyh.blesample

import android.app.Application
import com.hyh.ble.BleManager

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        BleManager.setReConnectCount(1,2000).init(this)
    }
}