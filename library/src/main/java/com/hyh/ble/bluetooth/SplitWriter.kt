package com.hyh.ble.bluetooth

import com.hyh.ble.BleManager
import com.hyh.ble.callback.BleWriteCallback
import com.hyh.ble.exception.BleException
import com.hyh.ble.utils.DataUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Queue

class SplitWriter(private val writeOperator: BleOperator) {
    private var mData: ByteArray? = null
    private var mCount = 0
    private var mIntervalBetweenTwoPackage: Long = 0
    private var mCallback: BleWriteCallback? = null
    private var mDataQueue: Queue<ByteArray>? = null
    private var mTotalNum = 0

    fun splitWrite(
        data: ByteArray,
        intervalBetweenTwoPackage: Long = 0,
        callback: BleWriteCallback,
        writeType: Int
    ) {
        mData = data
        mIntervalBetweenTwoPackage = intervalBetweenTwoPackage
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
            if (mDataQueue!!.isEmpty()) {
                channel.close()
            } else {
                writeOperator.launch {
                    ensureActive()//mIntervalBetweenTwoPackage可能为0
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
            if (mDataQueue!!.isEmpty()) {
                channel.close()
            } else {
                writeOperator.launch {
                    ensureActive() //mIntervalBetweenTwoPackage可能为0
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
                mDataQueue = DataUtil.splitPacketForByte(mData, mCount)
            }
            mTotalNum = mDataQueue!!.size
            var currentData: ByteArray? = null
            launch {
                channel.send(mDataQueue!!.poll())
            }
            channel.receiveAsFlow()
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