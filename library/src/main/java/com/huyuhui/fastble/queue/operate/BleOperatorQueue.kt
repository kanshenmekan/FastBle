package com.huyuhui.fastble.queue.operate

import com.huyuhui.fastble.bluetooth.BleBluetooth
import com.huyuhui.fastble.exception.BleCoroutineExceptionHandler
import com.huyuhui.fastble.queue.Queue
import com.huyuhui.fastble.queue.TaskResult
import com.huyuhui.fastble.utils.BleLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job

internal class BleOperatorQueue(private val bleBluetooth: BleBluetooth) :
    Queue<SequenceBleOperator>(),
    CoroutineScope by CoroutineScope(
        SupervisorJob(bleBluetooth.coroutineContext.job) + BleCoroutineExceptionHandler({ _, throwable ->
            BleLog.e("Ble operator queue:a coroutine error has occurred ${throwable.message}")
        })
    ) {
    override fun execute(task: SequenceBleOperator, channel: Channel<TaskResult>) {
        task.execute(bleBluetooth.bleDevice, channel)
    }
}