package com.example.hometempreaturemonitor

import android.util.Log
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.CRC32
import kotlin.concurrent.thread
import kotlin.math.E


class CommunicationCommandFormat {
    companion object {
        const val COMMAND_START = 0xEA
        const val LENGTH_OFFSET = 0x1
        const val HEADER_SIZE = 0x2
        const val DATA_OFFSET = 0x2
        const val CHECKSUM_SIZE = 0x4
    }

    private fun toInt32(bytes: ByteArray): Int {
        if (bytes.size != 4) {
            throw Exception("wrong len")
        }
        return ByteBuffer.wrap(bytes).int
    }

    private fun validateCommand(byteArray: ByteArray): ByteArray {
        if (byteArray[0].toUByte() != COMMAND_START.toUByte()) {
            Log.e(
                "CommunicationCommandFormat",
                "Invalid command start byte: Expected: $COMMAND_START " +
                        "Got: ${byteArray[0].toInt()}"
            )
            throw java.lang.Exception("Invalid command start byte")
        }

        val length = byteArray[LENGTH_OFFSET]
        if (byteArray.size != length + CHECKSUM_SIZE) {
            Log.e(
                "CommunicationCommandFormat",
                "Invalid command size: Expected: >= ${length + CHECKSUM_SIZE} " +
                        "Got: ${length.toInt()} "
            )
            throw java.lang.Exception("Invalid command size")
        }

        val data = byteArray.slice(0 until length).toByteArray()
        val crc32 = CRC32()
        crc32.update(data)
        val calculatedChecksum = crc32.value.toInt()
        val commandChecksum = toInt32(
            byteArray.slice(length until byteArray.size).toByteArray()
        ).toUInt() and 0xFFFFFFFF.toUInt()
        if (commandChecksum != calculatedChecksum.toUInt()) {
            Log.e(
                "CommunicationCommandFormat",
                "Packet data corrupted: Expected Checksum: $calculatedChecksum " +
                        "Got: $commandChecksum"
            )
            throw java.lang.Exception("Packet data corrupted")
        }
        Log.i(
            "CommunicationCommandFormat",
            "Message successfully validated: $calculatedChecksum " +
                    "Got: $commandChecksum"
        )

        return data
    }

    private fun getData(byteArray: ByteArray): ByteArray? {
        var data: ByteArray? = null
        try {
            data = validateCommand(byteArray)
        } catch (ex: Exception) {
            Log.e("ClientHandler", "Exception on Client execution: ${ex.message}")
        }
        return data
    }

    fun pushDataToStorage(byteArray: ByteArray) {
        val data = getData(byteArray)
        if (data != null) {
            thread { TemperatureDataStorage.instance.insert(Date(), data.toString()) }
        }
    }
}