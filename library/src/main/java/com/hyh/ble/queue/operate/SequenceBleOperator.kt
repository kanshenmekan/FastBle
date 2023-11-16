package com.hyh.ble.queue.operate

import com.hyh.ble.bluetooth.BleBluetooth
import com.hyh.ble.queue.Task
import com.hyh.ble.queue.TaskResult
import kotlinx.coroutines.channels.Channel

abstract class SequenceBleOperator(priority: Int, delay: Long) : Task(priority, delay) {
    abstract fun execute(bleBluetooth: BleBluetooth,channel: Channel<TaskResult>)
}