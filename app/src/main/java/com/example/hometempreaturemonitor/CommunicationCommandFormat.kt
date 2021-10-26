package com.example.hometempreaturemonitor

import android.util.Log
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.CRC32
import kotlin.concurrent.thread

data class TemperatureRecord(val date: Date, val temp: Float, val humidity: Float)

class CommunicationCommandFormat {
    companion object {
        const val COMMAND_START = 0xEA
        const val LENGTH_OFFSET = 0x1
        const val DATA_OFFSET = 0x2
        const val CHECKSUM_SIZE = 0x4
        const val TEMP_SIZE = 0x4
        const val TEMP_OFFSET = 0x0
        const val HUMIDITY_SIZE = 0x4
        const val HUMIDITY_OFFSET = TEMP_SIZE
    }

    private fun toInt32(bytes: ByteArray): Int {
        if (bytes.size != 4) {
            throw Exception("toInt32 conversion failed. Expected 4 bytes got: ${bytes.size}")
        }
        return ByteBuffer.wrap(bytes).int
    }

    private fun toFloat(bytes: ByteArray): Float {
        if (bytes.size != 4) {
            throw Exception("toFloat conversion failed. Expected 4 bytes got: ${bytes.size}")
        }
        return ByteBuffer.wrap(bytes).float
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
                        "Got: ${byteArray.size} "
            )
            throw java.lang.Exception("Invalid command size")
        }

        val dataWithHeader = byteArray.slice(0 until length).toByteArray()
        val crc32 = CRC32()
        crc32.update(dataWithHeader)
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

        return dataWithHeader.slice(DATA_OFFSET until dataWithHeader.size).toByteArray()
    }

    private fun getData(byteArray: ByteArray): ByteArray? {
        var data: ByteArray? = null
        try {
            data = validateCommand(byteArray)
        } catch (ex: Exception) {
            Log.e("CommunicationCommandFormat",
                "Exception on validateCommand execution: ${ex.message}")
        }
        return data
    }

    private fun getTempFromData(byteArray: ByteArray): Float {
        val tempBA = byteArray.slice(TEMP_OFFSET until TEMP_OFFSET + TEMP_SIZE).toByteArray()
        return toFloat(tempBA)
    }

    private fun getHumidityFromData(byteArray: ByteArray): Float {
        val humidityBA = byteArray.slice(HUMIDITY_OFFSET until HUMIDITY_OFFSET + HUMIDITY_SIZE).toByteArray()
        return toFloat(humidityBA)
    }

    private fun validateData(byteArray: ByteArray): Boolean {
        val sizeValid = byteArray.size >= TEMP_SIZE + HUMIDITY_SIZE
        if (!sizeValid) {
            Log.e("CommunicationCommandFormat", "Invalid data body size. " +
                    "Should be: ${TEMP_SIZE + HUMIDITY_SIZE} but Got: ${byteArray.size}")
        }
        return sizeValid
    }

    fun pushDataToStorage(byteArray: ByteArray) {
        val data = getData(byteArray)

        if (data != null) {
            if (validateData(data)) {
                try {
                    val temp = getTempFromData(data)
                    val humidity = getHumidityFromData(data)
                    thread { TemperatureDataStorage.instance.insert(Date(), temp, humidity) }
                } catch (ex: Exception) {
                    Log.e("CommunicationCommandFormat",
                        "Exception on getting values from data and pushing to db: ${ex.message}")
                }


            }
        }
    }
}