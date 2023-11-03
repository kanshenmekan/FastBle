package com.hyh.ble.bluetooth

import com.hyh.ble.BleManager
import com.hyh.ble.callback.BleWriteCallback
import com.hyh.ble.exception.BleException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList
import java.util.Queue
import kotlin.math.max

class SplitWriter(private var writeOperator: BleOperator) {
    private var mData: ByteArray? = null
    private var mCount = 0
    private var mSendNextWhenLastSuccess = false
    private var mIntervalBetweenTwoPackage: Long = 0
    private var mCallback: BleWriteCallback? = null
    private var mDataQueue: Queue<ByteArray>? = null
    private var mTotalNum = 0

    fun splitWrite(
        data: ByteArray,
        sendNextWhenLastSuccess: Boolean,
        intervalBetweenTwoPackage: Long = 0,
        callback: BleWriteCallback,
        writeType: Int
    ) {
        mData = data
        mSendNextWhenLastSuccess = sendNextWhenLastSuccess
        mIntervalBetweenTwoPackage =
            if (mSendNextWhenLastSuccess) intervalBetweenTwoPackage else max(
                intervalBetweenTwoPackage,
                10
            )
        mCallback = callback
        mCount = BleManager.splitWriteNum
        splitWrite(writeType)
    }

    val channel = Channel<ByteArray?>()

    private val callback = object : BleWriteCallback() {
        override fun onWriteSuccess(
            current: Int,
            total: Int,
            justWrite: ByteArray?,
            data: ByteArray?
        ) {
            val position = mTotalNum - mDataQueue!!.size
            mCallback?.onWriteSuccess(position, mTotalNum, justWrite, mData)
            if (mSendNextWhenLastSuccess)
                if (mDataQueue!!.isEmpty()) {
                    channel.close()
                } else {
                    writeOperator.launch {
                        delay(mIntervalBetweenTwoPackage)
                        channel.trySend(mDataQueue!!.poll())
                    }
                }
        }

        override fun onWriteFailure(
            exception: BleException?,
            current: Int,
            total: Int,
            justWrite: ByteArray?,
            data: ByteArray?,
            isTotalFail: Boolean
        ) {
            val position = mTotalNum - mDataQueue!!.size
            mCallback?.onWriteFailure(
                exception,
                position,
                mTotalNum,
                data,
                mData,
                mDataQueue?.isEmpty() ?: true
            )
            if (mSendNextWhenLastSuccess)
                if (mDataQueue!!.isEmpty()) {
                    channel.close()
                } else {
                    writeOperator.launch {
                        delay(mIntervalBetweenTwoPackage)
                        channel.trySend(mDataQueue!!.poll())
                    }
                }
        }

    }

    private fun splitWrite(writeType: Int) {
        requireNotNull(mData) { "data is Null!" }
        require(mCount >= 1) { "split count should higher than 0!" }
        writeOperator.launch {
            withContext(Dispatchers.IO) {
                mDataQueue = splitPacketForByte(mData, mCount)
            }
            mTotalNum = mDataQueue!!.size
            if (!mSendNextWhenLastSuccess) {
                var currentData: ByteArray? = null
                getDataFlow()
                    .onCompletion {
                        withContext(NonCancellable + Dispatchers.Main) {
                            if (mDataQueue!!.isNotEmpty()) {
                                val position = mTotalNum - mDataQueue!!.size
                                mCallback?.onWriteFailure(
                                    BleException.OtherException("CoroutineScope Cancelled when sending"),
                                    position,
                                    mTotalNum,
                                    currentData,
                                    mData,
                                    true
                                )
                            }
                            mDataQueue?.clear()
                            mCallback = null
                            mData = null
                        }
                    }
                    .collect {
                        currentData = it
                        writeOperator.writeCharacteristic(
                            it,
                            callback,
                            writeOperator.mCharacteristic!!.uuid.toString(),
                            writeType
                        )
                    }
            } else {
                var currentData: ByteArray? = null
                launch {
                    channel.send(mDataQueue!!.poll())
                }
                channel.consumeAsFlow()
                    .onCompletion {
                        withContext(NonCancellable + Dispatchers.Main) {
                            if (mDataQueue!!.isNotEmpty()) {
                                val position = mTotalNum - mDataQueue!!.size
                                mCallback?.onWriteFailure(
                                    BleException.OtherException("CoroutineScope Cancelled when sending"),
                                    position,
                                    mTotalNum,
                                    currentData,
                                    mData,
                                    true
                                )
                            }
                            mDataQueue?.clear()
                            mCallback = null
                            mData = null
                        }
                    }
                    .collect {
                        currentData = it
                        writeOperator.writeCharacteristic(
                            it,
                            callback,
                            writeOperator.mCharacteristic!!.uuid.toString(),
                            writeType
                        )
                    }
            }
        }
    }

    private fun getDataFlow(): Flow<ByteArray> {
        return flow {
            repeat(mTotalNum) {
                if (mDataQueue!!.peek() != null) {
                    emit(mDataQueue!!.poll() as ByteArray)
                    if (it < mTotalNum)
                        delay(mIntervalBetweenTwoPackage)
                }
            }
        }
    }


    private fun splitPacketForByte(data: ByteArray?, length: Int): Queue<ByteArray> {
        val dataInfoQueue: Queue<ByteArray> = LinkedList()
        if (data != null) {
            var index = 0
            do {
                val surplusData = ByteArray(data.size - index)
                var currentData: ByteArray
                System.arraycopy(data, index, surplusData, 0, data.size - index)
                if (surplusData.size <= length) {
                    currentData = ByteArray(surplusData.size)
                    System.arraycopy(surplusData, 0, currentData, 0, surplusData.size)
                    index += surplusData.size
                } else {
                    currentData = ByteArray(length)
                    System.arraycopy(data, index, currentData, 0, length)
                    index += length
                }
                dataInfoQueue.offer(currentData)
            } while (index < data.size)
        }
        return dataInfoQueue
    }
}