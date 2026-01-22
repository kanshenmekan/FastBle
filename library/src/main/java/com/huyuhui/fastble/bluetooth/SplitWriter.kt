package com.huyuhui.fastble.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import com.huyuhui.fastble.callback.BleWriteCallback
import com.huyuhui.fastble.data.BleDevice
import com.huyuhui.fastble.exception.BleException
import com.huyuhui.fastble.utils.DataUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SplitWriter(private val writeOperator: BleWriteOperator) {
    private lateinit var mData: ByteArray
    private var mIntervalBetweenTwoPackage: Long = 0
    private var mContinueWhenLastFail: Boolean = false
    private var mCallback: BleWriteCallback? = null
    private var mTotalNum = 0

    //避免多次调用onWriteFailure
    fun splitWrite(
        data: ByteArray,
        splitNum: Int,
        continueWhenLastFail: Boolean,
        intervalBetweenTwoPackage: Long = 0,
        callback: BleWriteCallback?,
        writeType: Int,
    ) {
        mData = data
        mContinueWhenLastFail = continueWhenLastFail
        mIntervalBetweenTwoPackage = intervalBetweenTwoPackage
        mCallback = callback
        splitWrite(splitNum, writeType)
    }

    private fun splitWrite(splitNum: Int, writeType: Int) {
        require(splitNum >= 1) { "split count should higher than 0!" }
        writeOperator.launch {
            val dataQueue = withContext(Dispatchers.IO) {
                DataUtil.splitPacketForByte(mData, splitNum)
            }
            mTotalNum = dataQueue.size
            var currentData: ByteArray? = null
            var currentPosition = 0
            dataQueue.asFlow()
                .onCompletion {
                    dataQueue.clear()
                    //不是被主动取消的，是协程被取消了,并且不是等待结果期间。如果hasTask，替换writeOperator会抛异常
                    if (it is CancellationException && it.cause == null) {
                        withContext(NonCancellable + Dispatchers.Main) {
                            mCallback?.onWriteFailure(
                                writeOperator.bleDevice,
                                writeOperator.mCharacteristic,
                                BleException.OtherException(
                                    BleException.COROUTINE_SCOPE_CANCELLED,
                                    "CoroutineScope Cancelled when sending"
                                ),
                                currentPosition,
                                mTotalNum,
                                currentData,
                                mData,
                                true
                            )
                            mCallback = null
                        }
                    }
                }
                .collectIndexed { position, bytes ->
                    currentData = bytes
                    currentPosition = position + 1
                    val result = writeDataForResult(bytes, writeType = writeType, currentPosition)
                    if (position < dataQueue.size - 1 && mIntervalBetweenTwoPackage > 0) {
                        delay(mIntervalBetweenTwoPackage)
                    }
                    if (!result && !mContinueWhenLastFail) {
                        cancel("send $position error", Throwable())
                    }
                }
        }
    }

    private suspend fun writeDataForResult(
        data: ByteArray,
        writeType: Int,
        position: Int
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val wrappedCallback = object : BleWriteCallback() {
            override fun onWriteSuccess(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic,
                current: Int,
                total: Int,
                justWrite: ByteArray,
                data: ByteArray,
            ) {
                mCallback?.onWriteSuccess(
                    bleDevice,
                    characteristic,
                    position,
                    mTotalNum,
                    justWrite,
                    mData
                )
                deferred.complete(true)
            }

            override fun onWriteFailure(
                bleDevice: BleDevice,
                characteristic: BluetoothGattCharacteristic?,
                exception: BleException,
                current: Int,
                total: Int,
                justWrite: ByteArray?,
                data: ByteArray?,
                isTotalFail: Boolean,
            ) {
                mCallback?.onWriteFailure(
                    bleDevice,
                    characteristic,
                    exception,
                    position,
                    mTotalNum,
                    data,
                    mData,
                    !mContinueWhenLastFail || position == mTotalNum
                )
                deferred.complete(false)
            }
        }
        writeOperator.writeCharacteristic(data, wrappedCallback, writeType)
        return deferred.await()
    }
}