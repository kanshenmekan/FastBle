package com.huyuhui.fastble.utils

import java.util.zip.CRC32
@Suppress("unused","FunctionName")
object CrcUtils {
    @JvmStatic
    fun CRC8(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x00
        val wCPoly = 0x07
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c07 = wCRCin shr 7 and 1 == 1
                wCRCin = wCRCin shl 1
                if (c07 xor bit) wCRCin = wCRCin xor wCPoly
            }
        }
        wCRCin = wCRCin and 0xFF
        return wCRCin xor 0x00
    }

    @JvmStatic
    fun CRC8_DARC(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x00
        // Integer.reverse(0x39) >>> 24
        val wCPoly = 0x9C
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0xFF)
            for (j in 0..7) {
                if (wCRCin and 0x01 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0x00
    }

    @JvmStatic
    fun CRC8_ITU(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x00
        val wCPoly = 0x07
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c07 = wCRCin shr 7 and 1 == 1
                wCRCin = wCRCin shl 1
                if (c07 xor bit) wCRCin = wCRCin xor wCPoly
            }
        }
        wCRCin = wCRCin and 0xFF
        return wCRCin xor 0x55
    }

    @JvmStatic
    fun CRC8_MAXIM(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x00
        // Integer.reverse(0x31) >>> 24
        val wCPoly = 0x8C
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0xFF)
            for (j in 0..7) {
                if (wCRCin and 0x01 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0x00
    }

    @JvmStatic
    fun CRC8_ROHC(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0xFF
        // Integer.reverse(0x07) >>> 24
        val wCPoly = 0xE0
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0xFF)
            for (j in 0..7) {
                if (wCRCin and 0x01 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0x00
    }

    @JvmStatic
    fun CRC16_IBM(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x0000
        // Integer.reverse(0x8005) >>> 16
        val wCPoly = 0xA001
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0x00FF)
            for (j in 0..7) {
                if (wCRCin and 0x0001 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0x0000
    }

    @JvmStatic
    fun CRC16_CCITT(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x0000
        // Integer.reverse(0x1021) >>> 16
        val wCPoly = 0x8408
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0x00FF)
            for (j in 0..7) {
                if (wCRCin and 0x0001 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0x0000
    }

    @JvmStatic
    fun CRC16_CCITT_FALSE(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0xFFFF
        val wCPoly = 0x1021
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c15 = wCRCin shr 15 and 1 == 1
                wCRCin = wCRCin shl 1
                if (c15 xor bit) wCRCin = wCRCin xor wCPoly
            }
        }
        wCRCin = wCRCin and 0xFFFF
        return wCRCin xor 0x0000
    }

    @JvmStatic
    fun CRC16_DECT_R(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x0000
        val wCPoly = 0x0589
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c15 = wCRCin shr 15 and 1 == 1
                wCRCin = wCRCin shl 1
                if (c15 xor bit) wCRCin = wCRCin xor wCPoly
            }
        }
        wCRCin = wCRCin and 0xFFFF
        return wCRCin xor 0x0001
    }

    @JvmStatic
    fun CRC16_DECT_X(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x0000
        val wCPoly = 0x0589
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c15 = wCRCin shr 15 and 1 == 1
                wCRCin = wCRCin shl 1
                if (c15 xor bit) wCRCin = wCRCin xor wCPoly
            }
        }
        wCRCin = wCRCin and 0xFFFF
        return wCRCin xor 0x0000
    }

    @JvmStatic
    fun CRC16_DNP(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x0000
        // Integer.reverse(0x3D65) >>> 16
        val wCPoly = 0xA6BC
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0x00FF)
            for (j in 0..7) {
                if (wCRCin and 0x0001 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0xFFFF
    }

    @JvmStatic
    fun CRC16_GENIBUS(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0xFFFF
        val wCPoly = 0x1021
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c15 = wCRCin shr 15 and 1 == 1
                wCRCin = wCRCin shl 1
                if (c15 xor bit) wCRCin = wCRCin xor wCPoly
            }
        }
        wCRCin = wCRCin and 0xFFFF
        return wCRCin xor 0xFFFF
    }

    @JvmStatic
    fun CRC16_MAXIM(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x0000
        // Integer.reverse(0x8005) >>> 16
        val wCPoly = 0xA001
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0x00FF)
            for (j in 0..7) {
                if (wCRCin and 0x0001 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0xFFFF
    }

    @JvmStatic
    fun CRC16_MODBUS(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0xFFFF
        // Integer.reverse(0x8005) >>> 16
        val wCPoly = 0xA001
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0x00FF)
            for (j in 0..7) {
                if (wCRCin and 0x0001 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0x0000
    }

    @JvmStatic
    fun CRC16_USB(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0xFFFF
        // Integer.reverse(0x8005) >>> 16
        val wCPoly = 0xA001
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0x00FF)
            for (j in 0..7) {
                if (wCRCin and 0x0001 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0xFFFF
    }

    @JvmStatic
    fun CRC16_X25(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0xFFFF
        // Integer.reverse(0x1021) >>> 16
        val wCPoly = 0x8408
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toInt() and 0x00FF)
            for (j in 0..7) {
                if (wCRCin and 0x0001 != 0) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0xFFFF
    }

    @JvmStatic
    fun CRC16_XMODEM(source: ByteArray, offset: Int = 0, length: Int = source.size): Int {
        var wCRCin = 0x0000
        val wCPoly = 0x1021
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c15 = wCRCin shr 15 and 1 == 1
                wCRCin = wCRCin shl 1
                if (c15 xor bit) wCRCin = wCRCin xor wCPoly
            }
        }
        wCRCin = wCRCin and 0xFFFF
        return wCRCin xor 0x0000
    }

    @JvmStatic
    fun CRC32(source: ByteArray, offset: Int = 0, length: Int = source.size): Long {
        val crc32c = CRC32()
        val bytes = source.copyOfRange(offset, offset + length)
        crc32c.update(bytes)
        return crc32c.value
    }

    @JvmStatic
    fun CRC32_B(source: ByteArray, offset: Int = 0, length: Int = source.size): Long {
        var wCRCin = 0xFFFFFFFFL
        val wCPoly = 0x04C11DB7L
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = (source[i].toInt() shr (7 - j) and 1) == 1
                val c31 = wCRCin shr 31 and 1L == 1L
                wCRCin = wCRCin shl 1
                if (c31 xor bit) {
                    wCRCin = wCRCin xor wCPoly
                }
            }
        }
        wCRCin = wCRCin and 0xFFFFFFFFL
        return wCRCin xor 0xFFFFFFFFL
    }

    @JvmStatic
    fun CRC32_C(source: ByteArray, offset: Int = 0, length: Int = source.size): Long {
        var wCRCin = 0xFFFFFFFFL
        // Long.reverse(0x1EDC6F41L) >>> 32
        val wCPoly = 0x82F63B78L

        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toLong() and 0x000000FFL)
            for (j in 0..7) {
                if (wCRCin and 0x00000001L != 0L) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0xFFFFFFFFL
    }

    @JvmStatic
    fun CRC32_D(source: ByteArray, offset: Int = 0, length: Int = source.size): Long {
        var wCRCin = 0xFFFFFFFFL
        // Long.reverse(0xA833982BL) >>> 32
        val wCPoly = 0xD419CC15L
        for (i in offset until offset + length) {
            wCRCin = wCRCin xor (source[i].toLong() and 0x000000FFL)
            for (j in 0..7) {
                if (wCRCin and 0x00000001L != 0L) {
                    wCRCin = wCRCin shr 1
                    wCRCin = wCRCin xor wCPoly
                } else {
                    wCRCin = wCRCin shr 1
                }
            }
        }
        return wCRCin xor 0xFFFFFFFFL
    }

    @JvmStatic
    fun CRC32_MPEG_2(source: ByteArray, offset: Int = 0, length: Int = source.size): Long {
        var wCRCin = 0xFFFFFFFFL
        val wCPoly = 0x04C11DB7L
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c31 = wCRCin shr 31 and 1L == 1L
                wCRCin = wCRCin shl 1
                if (c31 xor bit) {
                    wCRCin = wCRCin xor wCPoly
                }
            }
        }
        wCRCin = wCRCin and 0xFFFFFFFFL
        return wCRCin xor 0x00000000L
    }

    @JvmStatic
    fun CRC32_POSIX(source: ByteArray, offset: Int = 0, length: Int = source.size): Long {
        var wCRCin = 0x00000000L
        val wCPoly = 0x04C11DB7L
        for (i in offset until offset + length) {
            for (j in 0..7) {
                val bit = source[i].toInt() shr 7 - j and 1 == 1
                val c31 = wCRCin shr 31 and 1L == 1L
                wCRCin = wCRCin shl 1
                if (c31 xor bit) {
                    wCRCin = wCRCin xor wCPoly
                }
            }
        }
        wCRCin = wCRCin and 0xFFFFFFFFL
        return wCRCin xor 0xFFFFFFFFL
    }
}