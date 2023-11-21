package com.huyuhui.blesample

import android.app.Application
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.common.BleConnectStrategy

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        BleManager.apply {
            bleConnectStrategy = BleConnectStrategy().apply {
                connectOverTime = 10000
                setReConnectCount(1,2000)
            }
        }.init(this)
    }
}