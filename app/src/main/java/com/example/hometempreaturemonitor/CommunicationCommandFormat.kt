package com.example.hometempreaturemonitor

import android.util.Log
import java.nio.ByteBuffer
import java.util.zip.CRC32


class CommunicationCommandFormat {
    companion object {
        const val COMMAND_START = 0xEA
        const val LENGTH_OFFSET = 0x1
        const val HEADER_SIZE = 0x2
        const val DATA_OFFSET = 0x2
        const val CHECKSUM_SIZE = 0x4
    }

    private fun toInt32(bytes:ByteArray):Int {
        if (bytes.size != 4) {
            throw Exception("wrong len")
        }
//        bytes.reverse()
        return ByteBuffer.wrap(bytes).int
    }

    fun validate(byteArray: ByteArray): Boolean {
        if (byteArray[0].toUByte() != COMMAND_START.toUByte()) {
            Log.i("CommunicationCommandFormat",
                "Invalid command start byte: Expected: $COMMAND_START " +
                        "Got: ${byteArray[0].toInt()}")
            return false
        }

        val length = byteArray[LENGTH_OFFSET]
        if (byteArray.size != length + CHECKSUM_SIZE) {
            Log.i("CommunicationCommandFormat",
                "Invalid command size: Expected: >= ${length + CHECKSUM_SIZE} " +
                        "Got: ${length.toInt()} ")
            return false
        }

        val data = byteArray.slice(0 until length).toByteArray()
        val crc32 = CRC32()
        crc32.update(data)
        val calculatedChecksum = crc32.value.toInt()
        val commandChecksum = toInt32(byteArray.slice(length until byteArray.size).toByteArray()).toUInt() and 0xFFFFFFFF.toUInt()
        if (commandChecksum != calculatedChecksum.toUInt()) {
            Log.i("CommunicationCommandFormat",
                "Packet data corrupted: Expected Checksum: $calculatedChecksum " +
                        "Got: $commandChecksum")
            return false
        }
        Log.i("CommunicationCommandFormat",
            "Message successfully validated: $calculatedChecksum " +
                    "Got: $commandChecksum")

        return true
    }
}