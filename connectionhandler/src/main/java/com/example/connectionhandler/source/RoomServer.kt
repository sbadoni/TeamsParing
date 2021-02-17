package com.example.connectionhandler.source

import android.util.Log
import com.example.connectionhandler.data.PORT
import com.google.gson.Gson
import java.io.IOException
import java.net.ServerSocket

private const val TAG = "RoomServer"

class RoomServer(private val gSon: Gson) {
    private var roomServerSocket: ServerSocket? = null
    private var roomServerThread: Thread = Thread(RoomServerRunnable())

    init {
        roomServerThread.start()
    }

    fun tearDown() {
        roomServerThread.interrupt()
        try {
            roomServerSocket?.close()
        } catch (ioe: IOException) {
            Log.e(TAG, "Error when closing room server socket")
        }
    }

    inner class RoomServerRunnable : Runnable {
        override fun run() {
            try {
                roomServerSocket = ServerSocket(PORT)
                while (!Thread.currentThread().isInterrupted) {
                    Log.i(TAG, "Room Server Socket Created, awaiting connection")
                    roomServerSocket!!.accept()?.let { consoleSocket ->
                        Log.i(TAG, "Connected console ${consoleSocket.inetAddress}")
                        RoomRequestResponseHandler(consoleSocket, gSon)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating ServerSocket: ", e)
            } finally {
                Log.i(TAG, "Shutting down the room server")
            }
        }
    }
}

