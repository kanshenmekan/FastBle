package com.huyuhui.fastble.queue.operate

import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.queue.Task

abstract class SequenceBleOperator(priority: Int) : Task(priority) {
    abstract suspend fun execute(bleDevice: BleDevice)
}