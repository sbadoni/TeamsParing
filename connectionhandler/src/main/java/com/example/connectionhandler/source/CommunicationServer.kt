package com.example.connectionhandler.source

import android.util.Log
import com.example.connectionhandler.data.PORT
import com.google.gson.Gson
import java.io.IOException
import java.net.ServerSocket

private const val TAG = "CommunicationServer"
class CommunicationServer(private val gSon: Gson) {
    private var mServerSocket: ServerSocket? = null
    private var mThread: Thread = Thread(CommunicationServerThread())

    init {
        mThread.start()
    }

    fun tearDown() {
        mThread.interrupt()
        try {
            mServerSocket?.close()
        } catch (ioe: IOException) {
            Log.e(TAG, "Error when closing server socket.")
        }
    }

    inner class CommunicationServerThread : Runnable {
        override fun run() {
            try {
                mServerSocket = ServerSocket(PORT)
                while (!Thread.currentThread().isInterrupted) {
                    Log.d(TAG, "ServerSocket Created, awaiting connection")
                    val client = mServerSocket!!.accept()
                    Log.d(TAG, "Connected.")
                    client?.let {
                        ServerResponseHandler(it, gSon)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating ServerSocket: ", e)
            }
        }
    }
}

