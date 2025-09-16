package com.huyuhui.fastble.utils

import android.util.Log

@Suppress("unused")
object BleLog {
    var isPrint = true

    @JvmStatic
    private val defaultTag = "FastBle"

    fun d(msg: String?) {
        if (isPrint && msg != null) Log.d(defaultTag, msg)
    }

    fun i(msg: String?) {
        if (isPrint && msg != null) Log.i(defaultTag, msg)
    }

    fun w(msg: String?) {
        if (isPrint && msg != null) Log.w(defaultTag, msg)
    }

    fun e(msg: String?) {
        if (isPrint && msg != null) Log.e(defaultTag, msg)
    }

    fun e(msg: String?, e: Throwable) {
        if (isPrint && msg != null) Log.e(defaultTag, msg, e)
    }
}