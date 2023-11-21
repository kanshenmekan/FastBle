package com.huyuhui.fastble.queue.operate

import com.huyuhui.fastble.bluetooth.BleBluetooth
import com.huyuhui.fastble.queue.Queue
import com.huyuhui.fastble.queue.TaskResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job

class BleOperatorQueue(private val bleBluetooth: BleBluetooth) : Queue<SequenceBleOperator>(),
    CoroutineScope by CoroutineScope(
        SupervisorJob(bleBluetooth.coroutineContext.job)
    ) {
    override fun execute(task: SequenceBleOperator, channel: Channel<TaskResult>) {
        task.execute(bleBluetooth, channel)
    }
}