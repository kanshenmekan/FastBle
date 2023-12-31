package com.huyuhui.fastble.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.PriorityBlockingQueue

abstract class Queue<T : Task> : CoroutineScope {
    private val channel = Channel<TaskResult>()
    private val taskComparator = Comparator<T> { task1, task2 ->
        if (task2.priority != task1.priority) {
            task2.priority.compareTo(task1.priority) // 逆序排列，优先级高的排在前面
        } else {
            task1.sequenceNum.compareTo(task2.sequenceNum)
        }
    }
    private val priorityQueue: PriorityBlockingQueue<T> =
        PriorityBlockingQueue(10, taskComparator)
    private var job: Job? = null

    val remainSize
        get() = priorityQueue.size

    fun offer(task: T): Boolean {
        return priorityQueue.offer(task)
    }

    fun startProcessingTasks() {
        resume()
    }

    //    abstract fun execute(task: T)
    abstract fun execute(task: T, channel: Channel<TaskResult>)
    fun clear() {
        priorityQueue.clear()
    }

    fun remove(task: T): Boolean {
        return priorityQueue.remove(task)
    }

    fun pause() {
        job?.cancel()
    }

    fun resume() {
        if (job?.isActive == true) {
            return
        }
        job = launch(Dispatchers.IO) {
            while (isActive) {
                val task = priorityQueue.take()
                withContext(Dispatchers.Main) {
                    execute(task, channel)
                }
                if (task.continuous) {
                    if (task.timeout > 0) {
                        withTimeoutOrNull(task.timeout) {
                            do {
                                val result = channel.receive()
                            } while (result.task != task)
                            return@withTimeoutOrNull
                        }
                    } else {
                        do {
                            val result = channel.receive()
                        } while (result.task != task)
                    }
                }
                delay(task.delay)
            }
        }
    }

    fun destroy() {
        channel.close()
        clear()
        cancel()
    }
}