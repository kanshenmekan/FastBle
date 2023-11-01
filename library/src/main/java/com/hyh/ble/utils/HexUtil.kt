package com.hyh.ble.utils

import java.util.Locale

object HexUtil {
    private val DIGITS_LOWER = charArrayOf(
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )

    private val DIGITS_UPPER = charArrayOf(
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )
    @JvmStatic
    fun encodeHex(data: ByteArray?): CharArray? {
        return encodeHex(data, true)
    }

    @JvmStatic
    fun encodeHex(data: ByteArray?, toLowerCase: Boolean): CharArray? {
        return encodeHex(data, if (toLowerCase) DIGITS_LOWER else DIGITS_UPPER)
    }

    private fun encodeHex(data: ByteArray?, toDigits: CharArray): CharArray? {
        if (data == null) return null
        val l = data.size
        val out = CharArray(l shl 1)
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = toDigits[(0xF0 and data[i].toInt()) ushr 4]
            out[j++] = toDigits[0x0F and data[i].toInt()]
            i++
        }
        return out
    }

    @JvmStatic
    fun encodeHexStr(data: ByteArray?): String? {
        return encodeHexStr(data, true)
    }
    @JvmStatic
    fun encodeHexStr(data: ByteArray?, toLowerCase: Boolean): String? {
        return encodeHexStr(data, if (toLowerCase) DIGITS_LOWER else DIGITS_UPPER)
    }


    private  fun encodeHexStr(data: ByteArray?, toDigits: CharArray): String? {
        return encodeHex(data, toDigits)?.let { String(it) }
    }
    @JvmStatic
    fun formatHexString(data: ByteArray?): String? {
        return formatHexString(data, false)
    }
    @JvmStatic
    fun formatHexString(data: ByteArray?, addSpace: Boolean): String? {
        if (data == null || data.isEmpty()) return null
        val sb = StringBuilder()
        for (i in data.indices) {
            var hex = Integer.toHexString(data[i].toInt() and 0xFF)
            if (hex.length == 1) {
                hex = "0$hex"
            }
            sb.append(hex)
            if (addSpace) sb.append(" ")
        }
        return sb.toString().trim { it <= ' ' }
    }

    fun decodeHex(data: CharArray): ByteArray {
        val len = data.size
        if (len and 0x01 != 0) {
            throw RuntimeException("Odd number of characters.")
        }
        val out = ByteArray(len shr 1)

        // two characters form the hex value.
        var i = 0
        var j = 0
        while (j < len) {
            var f = toDigit(data[j], j) shl 4
            j++
            f = f or toDigit(data[j], j)
            j++
            out[i] = (f and 0xFF).toByte()
            i++
        }
        return out
    }


    private fun toDigit(ch: Char, index: Int): Int {
        val digit = ch.digitToIntOrNull(16) ?: -1
        if (digit == -1) {
            throw RuntimeException(
                "Illegal hexadecimal character " + ch
                        + " at index " + index
            )
        }
        return digit
    }

    @JvmStatic
    fun hexStringToBytes(hexString: String?): ByteArray? {
        if (hexString.isNullOrEmpty()) {
            return null
        }
        var string = hexString.trim { it <= ' ' }
        string = string.uppercase(Locale.getDefault())
        val length = string.length / 2
        val hexChars = string.toCharArray()
        hexChars.forEach {
            if ("0123456789ABCDEF".indexOf(it) == -1)return null
        }
        val d = ByteArray(length)
        for (i in 0 until length) {
            val pos = i * 2
            d[i] =
                ((charToByte(hexChars[pos]).toInt() shl 4) or charToByte(hexChars[pos + 1]).toInt()).toByte()
        }
        return d
    }
    @JvmStatic
    fun charToByte(c: Char): Byte {
        return "0123456789ABCDEF".indexOf(c).toByte()
    }
    @JvmStatic
    fun extractData(data: ByteArray, position: Int): String? {
        return formatHexString(byteArrayOf(data[position]))
    }
}