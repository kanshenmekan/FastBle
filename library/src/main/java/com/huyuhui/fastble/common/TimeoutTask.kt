package com.huyuhui.fastble.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
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

    suspend fun start() {
        job?.apply {
            if (!isCompleted) {
                isSkip = true
                cancel(
                    CancellationException(
                        TimeoutThrowable.SkipError.message,
                        TimeoutThrowable.SkipError
                    )
                )
            } else {
                isSkip = false
            }
        }
        supervisorScope {
            job = launch(exceptionHandler) {
                coroutineContext.job.invokeOnCompletion {
                    onTimeoutResultCallBack?.onFinal(this@TimeoutTask, isSkip)
                }
                onTimeoutResultCallBack?.onStart(this@TimeoutTask)
                try {
                    delay(delayTime)
                    throw TimeoutThrowable.TimeOutError
                } catch (e: CancellationException) {
                    throw e.cause ?: Throwable()
                }
            }
        }
    }

    fun fail() {
        job?.cancel(
            CancellationException(
                TimeoutThrowable.ActiveError.message,
                TimeoutThrowable.ActiveError
            )
        )
    }

    fun success() {
        job?.cancel(
            CancellationException(
                TimeoutThrowable.Success.message,
                TimeoutThrowable.Success
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
        fun onFinal(task: TimeoutTask, isSkip: Boolean) {}
    }

    sealed class TimeoutThrowable(message: String? = null) : Throwable(message) {
        object Success : TimeoutThrowable("success") {
            private fun readResolve(): Any = Success
        }

        object ActiveError : TimeoutThrowable("error") {
            private fun readResolve(): Any = ActiveError
        }

        object TimeOutError : TimeoutThrowable("time out") {
            private fun readResolve(): Any = TimeOutError
        }

        object SkipError : TimeoutThrowable("skip") {
            private fun readResolve(): Any = SkipError
        }
    }
}