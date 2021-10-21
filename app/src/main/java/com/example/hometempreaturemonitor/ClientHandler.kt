package com.example.hometempreaturemonitor

import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

private var TERMINATE_BYTE: UByte = 0xeeu
private var START_BYTE: UByte = 0xeau

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

class ClientHandler(client: Socket?) {
    private val client: Socket? = client
    private val reader: BufferedInputStream = BufferedInputStream(client?.getInputStream())
    private val writer: OutputStream? = client?.getOutputStream()
    private var running: Boolean = false

    fun run() {
        running = true
        val baos: ByteArrayOutputStream = ByteArrayOutputStream()
        var started = false
        var messageLength = 0
        while (running && !Thread.currentThread().isInterrupted) {
            try {
                Log.i("ClientHandler","Available bytes: ${reader.available()}")
                while (!started) {
                    if (reader.available() > 0) {
                        started = true
                    }
                }
                if (reader.available() > 0) {
                    baos.write(reader.read())
                    if (baos.size() >= CommunicationCommandFormat.HEADER_SIZE) {
                        messageLength = baos.toByteArray()[
                                CommunicationCommandFormat.LENGTH_OFFSET].toInt() +
                                CommunicationCommandFormat.CHECKSUM_SIZE
                    }
                    if (baos.size() >= messageLength && messageLength > 0) {
                        Log.i("ClientHandler",
                            "Message successfully received: ${baos.toByteArray().toHexString()}")
                        if (CommunicationCommandFormat().validate(baos.toString())) {
                            thread { TemperatureDataStorage.instance.insert(Date(), baos.toString()) }
                        }
                        shutdown()
                    }
                } else {
                    Log.e("ClientHandler","Not Full message was received. " +
                            "Expected: ${messageLength}, Got: ${baos.size()}")
                    shutdown()
                }
            } catch (ex: Exception) {
                Log.e("ClientHandler","Exception on Client execution: ${ex.message}")
                shutdown()
            } finally {

            }
        }
        Log.e("ClientHandler","Thread interrupted or run finished")
    }
    private fun shutdown() {
        running = false
        client?.close()
        Log.i("ClientHandler","${client?.inetAddress?.hostAddress} closed the connection")
    }
}