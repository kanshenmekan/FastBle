package com.hyh.ble.queue.operate

import com.hyh.ble.bluetooth.BleBluetooth
import com.hyh.ble.queue.Task

abstract class SequenceBleOperator(priority: Int, delay: Long) : Task(priority, delay) {
    abstract fun execute(bleBluetooth: BleBluetooth)
}