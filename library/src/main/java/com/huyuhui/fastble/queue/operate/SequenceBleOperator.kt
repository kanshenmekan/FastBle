package com.huyuhui.fastble.queue.operate

import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.queue.Task
import com.huyuhui.fastble.queue.TaskResult
import kotlinx.coroutines.channels.Channel

abstract class SequenceBleOperator(priority: Int, delay: Long) : Task(priority, delay) {
     abstract fun execute(bleDevice: BleDevice, channel: Channel<TaskResult>)
}