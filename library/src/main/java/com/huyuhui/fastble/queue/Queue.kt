package com.huyuhui.fastble.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.PriorityBlockingQueue

internal abstract class Queue<T : Task> : CoroutineScope {
    /**
     * 某些情况发送失败太快，导致还没有调用receive的情况就开始trySend一直失败，给一个缓冲
     */
    private val channel = Channel<TaskResult>(1)
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

    /**
     * receive 可以收到在调用之前通过 trySend 发送的数据，前提是 trySend 成功发送了数据，并且 Channel 没有被关闭
     * 可能出现trySend比Receive调用更早的情况
     * 如果channel没有缓存，就会出现trySend失败，当continuous为true的时候，就会一直等待withTimeoutOrNull超时，然后取消receive
     * 等待priorityQueue.take()下一个任务来的时候，上一次receive已经被取消，又会卡在withTimeoutOrNull，如此循环。如果没有超时逻辑，会一直卡在receive
     *
     **/
    fun resume() {
        if (job?.isActive == true) {
            return
        }
        job = launch(Dispatchers.IO) {
            while (isActive) {
                val task = priorityQueue.take()

                withContext(Dispatchers.Main) {
                    //这里会在回调里面trySend
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
        coroutineContext.job.invokeOnCompletion {
            channel.close()
        }
        clear()
        cancel()
    }
}