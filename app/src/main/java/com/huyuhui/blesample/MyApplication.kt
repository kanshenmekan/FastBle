package com.huyuhui.blesample

import android.app.Application
import com.huyuhui.fastble.BleManager
import com.huyuhui.fastble.common.BleConnectStrategy
import com.huyuhui.fastble.common.BleFactory
import com.huyuhui.fastble.data.BleDevice

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BleManager.apply {
            enableLog(true)
            maxConnectCount = 5
            operateTimeout = 2000
            splitWriteNum = 20
            bleConnectStrategy = BleConnectStrategy.Builder().setConnectOverTime(10000)
                .setConnectBackpressureStrategy(BleConnectStrategy.CONNECT_BACKPRESSURE_DROP)
                .setReConnectCount(1).setReConnectInterval(2000).build()
            bleFactory = object :BleFactory{
                override fun generateUniqueKey(bleDevice: BleDevice): String {
                   return bleDevice.mac
                }
            }
        }.init(this)
    }
}