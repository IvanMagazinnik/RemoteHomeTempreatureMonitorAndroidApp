package com.example.hometempreaturemonitor

import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.Socket

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

class ClientHandler(client: Socket?) {
    private val client: Socket? = client
    private val reader: BufferedInputStream = BufferedInputStream(client?.getInputStream())
    private var running: Boolean = false

    fun run() {
        running = true
        val baos = ByteArrayOutputStream()
        var started = false
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
                } else {
                    Log.i("ClientHandler",
                        "Message successfully received: ${baos.toByteArray().toHexString()}")
                    CommunicationCommandFormat().pushDataToStorage(baos.toByteArray())
                    shutdown()
                }
            } catch (ex: Exception) {
                Log.e("ClientHandler","Exception on Client execution: ${ex.message}")
                shutdown()
            } finally {

            }
        }
        Log.i("ClientHandler","Thread interrupted or run finished")
    }
    private fun shutdown() {
        running = false
        client?.close()
        Log.i("ClientHandler","${client?.inetAddress?.hostAddress} closed the connection")
    }
}