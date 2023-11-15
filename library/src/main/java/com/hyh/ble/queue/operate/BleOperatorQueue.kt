package com.hyh.ble.queue.operate

import com.hyh.ble.bluetooth.BleBluetooth
import com.hyh.ble.queue.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
class BleOperatorQueue(private val bleBluetooth: BleBluetooth) : Queue<SequenceBleOperator>(),
    CoroutineScope by CoroutineScope(
        SupervisorJob(bleBluetooth.coroutineContext.job)
    ) {
    override fun execute(task: SequenceBleOperator) {
        task.execute(bleBluetooth)
    }

}