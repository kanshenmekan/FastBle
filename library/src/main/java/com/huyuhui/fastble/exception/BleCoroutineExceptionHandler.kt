package com.huyuhui.fastble.exception

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlin.coroutines.CoroutineContext

// 扩展 MainScope，返回一个包含 exceptionHandler 的作用域
fun CoroutineScope.withExceptionHandler(handler: CoroutineExceptionHandler): CoroutineScope {
    return CoroutineScope(coroutineContext + handler)
}

@Suppress("FunctionName")
fun BleMainScope(handler: (CoroutineContext, Throwable) -> Unit): CoroutineScope =
    MainScope().withExceptionHandler(BleCoroutineExceptionHandler(handler))

@Suppress("FunctionName")
inline fun BleCoroutineExceptionHandler(crossinline handler: (CoroutineContext, Throwable) -> Unit): CoroutineExceptionHandler =
    CoroutineExceptionHandler { coroutineContext, exception ->
        println("Caught exception: ${exception.message}")
        println("Stack trace:")
        exception.stackTrace.filter { it.className.startsWith("com.huyuhui.fastble") }
            .forEach { stackTraceElement ->
                println("  at $stackTraceElement")
            }
        // 打印完整的堆栈跟踪
        exception.stackTrace.forEach { stackTraceElement ->
            println("  at $stackTraceElement")
        }
        handler(coroutineContext, exception)
    }