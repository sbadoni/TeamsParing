package com.example.connectionhandler.source

import android.util.Log
import com.example.connectionhandler.data.Packet
import com.example.connectionhandler.data.SEND_SETTINGS_REQUEST
import com.example.connectionhandler.data.SEND_SETTINGS_RESPONSE
import com.example.connectionhandler.data.Settings
import com.google.gson.Gson
import java.io.*
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

private const val SERVER_TAG = "SERVER:- "
private const val HEARTBEAT_SEND_TIME = 5000

class ServerResponseHandler(
    private val connectedClientSocket: Socket,
    private val gSon: Gson
) {
    private val mSendThread: Thread = Thread(SendingThread())
    private var mRecThread: Thread = Thread(ReceivingThread())
    private val mMessageQueue: BlockingQueue<String> = ArrayBlockingQueue(10)
    private var lastKnowTimeForHeartBeat: Long = System.currentTimeMillis()

    init {
        mSendThread.start()
        mRecThread.start()

    }

    inner class SendingThread : Runnable {
        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    mMessageQueue.poll()?.let {
                        sendMessage(it)
                    }
                    if (System.currentTimeMillis() - lastKnowTimeForHeartBeat > HEARTBEAT_SEND_TIME) {
                        println("heartBeatRunnable")
                        mMessageQueue.put(gSon.toJson(Packet()))
                        lastKnowTimeForHeartBeat = System.currentTimeMillis()
                    }

                } catch (ie: InterruptedException) {
                    Log.d(SERVER_TAG, "Message sending loop interrupted, exiting")
                    tearDown()
                }
            }
            //Looper.loop()
        }
    }

    inner class ReceivingThread : Runnable {
        override fun run() {
            try {
                BufferedReader(
                    InputStreamReader(
                        connectedClientSocket.getInputStream()
                    )
                ).use { bufferReader ->
                    var messageStr: String? = bufferReader.readLine()
                    while (messageStr != null && !Thread.currentThread().isInterrupted) {
                        Log.d(SERVER_TAG, "Read from the stream: $messageStr")
                        val message = gSon.fromJson(messageStr, Packet::class.java)
                        message.request?.let { request ->
                            when (request) {
                                SEND_SETTINGS_REQUEST -> {
                                    println("SEND_SETTINGS_REQUEST")
                                    sendMessage(
                                        gSon.toJson(
                                            Packet(
                                                response = SEND_SETTINGS_RESPONSE,
                                                responseJson = gSon.toJson(
                                                    Settings(
                                                        "Mercury",
                                                        ""
                                                    )
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                        }
                        messageStr = bufferReader.readLine()
                    }
                }
            } catch (e: Exception) {
                Log.e(SERVER_TAG, "Server loop error: " + e.message)
                tearDown()
            }
        }
    }

    fun tearDown() {
        mSendThread.interrupt()
        mRecThread.interrupt()
        try {
            connectedClientSocket?.close()
        } catch (ioe: IOException) {
            Log.e(SERVER_TAG, "Error when closing server socket.")
        }
    }


    fun sendMessage(msg: String) {
        try {
            val socket = connectedClientSocket
            if (socket.getOutputStream() == null) {
                Log.d(SERVER_TAG, "Socket output stream is null");
            }

            val out = PrintWriter(
                BufferedWriter(
                    OutputStreamWriter(connectedClientSocket.getOutputStream())
                ), true
            )
            out.println(msg)
            out.flush()
        } catch (e: Exception) {
            Log.d(SERVER_TAG, "Error", e);
        }
    }
}
