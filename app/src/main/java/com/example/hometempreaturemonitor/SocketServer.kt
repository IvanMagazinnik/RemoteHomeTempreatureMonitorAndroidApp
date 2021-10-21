package com.example.hometempreaturemonitor

import android.util.Log
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

private const val SERVER_PORT = 40765

class SocketServer {
    private var server: ServerSocket? = null
    private var isAlive = false

    fun init() {
        try {
            server = ServerSocket(63545)
            isAlive = true
            keepAlive()
            Log.i("SocketServer","Server is running on port ${server?.localPort}")
        } catch (ex: Exception) {
            isAlive = false
            Log.e("SocketServer","Exception on server init: ${ex.message}")
        } finally {

        }

    }

    private fun keepAlive() {
        thread {
            while(true) {
                if (server?.isClosed == true || !isAlive ) {
                    init()
                    Thread.sleep(10000)
                }
            }
        }
    }

    fun run() {
        try {
            while (true) {
                Log.i("SocketServer", "Before client connected")
                var client: Socket? = null
                try {
                    client = server?.accept()
                } catch (ex: Exception) {
                    Log.e("SocketServer","Exception on accepting server: ${ex.message}")
                    isAlive = false
                    continue
                }
                try {
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