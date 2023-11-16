package com.hyh.ble.queue.operate

import com.hyh.ble.bluetooth.BleBluetooth
import com.hyh.ble.queue.Queue
import com.hyh.ble.queue.TaskResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job
class BleOperatorQueue(private val bleBluetooth: BleBluetooth) : Queue<SequenceBleOperator>(),
    CoroutineScope by CoroutineScope(
        SupervisorJob(bleBluetooth.coroutineContext.job)
    ) {
    override fun execute(task: SequenceBleOperator, channel: Channel<TaskResult>) {
        task.execute(bleBluetooth,channel)
    }
}