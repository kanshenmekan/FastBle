package com.huyuhui.fastble.common

import com.huyuhui.fastble.data.BleDevice

interface BleFactory {
    fun generateUniqueKey(bleDevice: BleDevice): String
}