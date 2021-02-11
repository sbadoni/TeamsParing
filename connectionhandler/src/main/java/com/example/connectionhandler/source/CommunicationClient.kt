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

private const val CLIENT_TAG = "CommunicationClient"

class CommunicationClient(
    private val client: Socket,
    private val gSon: Gson
) {

    private val mSendThread: Thread = Thread(SendingThread())
    private var mRecThread: Thread? = null
    val mMessageQueue: BlockingQueue<String> = ArrayBlockingQueue<String>(10)

    init {
        mSendThread.start()
    }

    inner class SendingThread : Runnable {

        override fun run() {
            try {
                mRecThread = Thread(ReceivingThread())
                mRecThread!!.start()
            } catch (e: Exception) {
                Log.d(CLIENT_TAG, "Initializing socket failed, IOE.", e)
            }

            while (true) {
                try {
                    val msg = mMessageQueue.take()
                    sendMessage(msg)
                } catch (ie: InterruptedException) {
                    Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting")
                }
            }
        }
    }

    inner class ReceivingThread : Runnable {

        override fun run() {
            val input: BufferedReader
            try {
                input = BufferedReader(
                    InputStreamReader(
                        client.getInputStream()
                    )
                )
                while (!Thread.currentThread().isInterrupted) {
                    val messageStr: String? = input.readLine()
                    if (messageStr != null) {
                        Log.d(CLIENT_TAG, "Read from the stream: $messageStr")
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
                        message.response?.let { response ->
                            when (response) {
                                SEND_SETTINGS_RESPONSE -> {
                                    message.responseJson?.let {
                                        val settings = gSon.fromJson(it, Settings::class.java)
                                        println("GOT: DEVICE-NAME ${settings.deviceName}")
                                        println("GOT: OTHER-PROPERTIES ${settings.otherProperties}")
                                    }
                                }
                                else -> println("Unknown Response...")
                            }
                        }
                    } else {
                        Log.d(CLIENT_TAG, "The null message...!")
                        break
                    }
                }
                input.close()
            } catch (e: Exception) {
                Log.e(CLIENT_TAG, "Server loop error: ", e);
            }
        }
    }

    fun sendMessage(msg: String) {
        try {
            val socket = client
            if (socket.getOutputStream() == null) {
                Log.d(CLIENT_TAG, "Socket output stream is null");
            }

            val out = PrintWriter(
                BufferedWriter(
                    OutputStreamWriter(client.getOutputStream())
                ), true
            )
            out.println(msg)
            out.flush()
        } catch (e: Exception) {
            Log.d(CLIENT_TAG, "Error", e);
        }
    }

    fun tearDown() {
        try {
            client.close()
        } catch (ioe: IOException) {
            Log.e(CLIENT_TAG, "Error when closing server socket.")
        }
    }
}

