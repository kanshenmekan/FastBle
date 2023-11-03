package com.hyh.ble.utils

import java.util.LinkedList
import java.util.Queue

object DataUtil {
    fun splitPacketForByte(data: ByteArray?, length: Int): Queue<ByteArray> {
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