package com.example.hometempreaturemonitor

import android.util.Log
import java.net.ServerSocket
import kotlin.concurrent.thread

private const val SERVER_PORT = 40765

class SocketServer {
    private var server: ServerSocket? = null

    fun init() {
        try {
            server = ServerSocket(63545)
            Log.i("SocketServer","Server is running on port ${server?.localPort}")
        } catch (ex: Exception) {
            Log.e("SocketServer","Exception on server init: ${ex.message}")
        } finally {

        }

    }

    fun run() {
        try {
            while (true) {
                Log.i("SocketServer", "Before client connected")
                try {
                    val client = server?.accept()

                    Log.i("SocketServer", "Client connected: ${client?.inetAddress?.hostAddress}")

                    // Run client in it's own thread.
                    thread {
                        val tr = thread { ClientHandler(client).run() };
                        tr.join(10000);
                        if (tr.isAlive) {
                            tr.interrupt()
                        }
                    }
                } catch (ex: Exception) {
                    Log.e("SocketServer","Exception on Client execution: ${ex.message}")
                }
            }
        } catch (ex: Exception) {
            Log.e("SocketServer","Exception on client run: ${ex.message}")
        } finally {

        }
    }

}