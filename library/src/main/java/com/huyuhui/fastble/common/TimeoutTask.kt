package com.huyuhui.fastble.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@Suppress("unused")
class TimeoutTask(
    private val delayTime: Long,
    var onTimeoutResultCallBack: OnResultCallBack? = null
) {
    private var job: Job? = null
    private var isSkip: Boolean = false
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        when (throwable) {
            is TimeoutThrowable.SkipError -> {
                onTimeoutResultCallBack?.onSkip(this)
            }

            is TimeoutThrowable.Success -> {
                onTimeoutResultCallBack?.onSuccess(this)
            }

            is TimeoutThrowable.ActiveError -> {
                onTimeoutResultCallBack?.onError(this, throwable, true)
            }

            is TimeoutThrowable.TimeOutError -> {
                onTimeoutResultCallBack?.onError(this, throwable, false)
            }

            else -> {

            }
        }
    }

    fun start(scope: CoroutineScope) {
        job?.takeIf { it.isActive }?.let {
            onTimeoutResultCallBack?.onSkip(this@TimeoutTask)
            it.cancel()
        }
        job = scope.launch {
            supervisorScope {
                launch(exceptionHandler) {
                    coroutineContext.job.invokeOnCompletion {
                        onTimeoutResultCallBack?.onFinal(it, this@TimeoutTask)
                    }
                    onTimeoutResultCallBack?.onStart(this@TimeoutTask)
                    try {
                        delay(delayTime)
                        throw TimeoutThrowable.TimeOutError()
                    } catch (e: CancellationException) {
                        throw e.cause ?: Throwable()
                    }
                }
            }
        }
    }

    fun fail() {
        job?.takeIf { it.isActive }?.cancel(
            CancellationException(
                TimeoutThrowable.ActiveError().message,
                TimeoutThrowable.ActiveError()
            )
        )
    }

    fun success() {
        job?.takeIf { it.isActive }?.cancel(
            CancellationException(
                TimeoutThrowable.Success().message,
                TimeoutThrowable.Success()
            )
        )
    }

    fun hasTask(): Boolean {
        return job?.isActive ?: false
    }

    /**
     * 不回调成功或者失败
     */
    fun cancel() {
        job?.takeIf { it.isActive }?.cancel()
    }

    interface OnResultCallBack {
        fun onStart(task: TimeoutTask) {}
        fun onError(task: TimeoutTask, e: Throwable?, isActive: Boolean) {}
        fun onSuccess(task: TimeoutTask) {}
        fun onSkip(task: TimeoutTask) {}
        fun onFinal(e: Throwable?, task: TimeoutTask) {}
    }

    sealed class TimeoutThrowable(message: String? = null) : Throwable(message) {
        class Success : TimeoutThrowable("success")

        class ActiveError : TimeoutThrowable("error")

        class TimeOutError : TimeoutThrowable("time out")

        class SkipError : TimeoutThrowable("skip")
    }
}