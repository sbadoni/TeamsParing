package com.example.connectionhandler.source

import android.util.Log
import com.example.connectionhandler.data.Packet
import com.example.connectionhandler.data.SEND_SETTINGS_RESPONSE
import com.example.connectionhandler.data.SYNC_SETTINGS_REQUEST
import com.example.connectionhandler.data.Settings
import com.google.gson.Gson
import java.io.*
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

private const val REQUEST_RESPONSE_TAG = "RoomReqResponseHandler"
private const val HEARTBEAT_SEND_TIME = 5000

class RoomRequestResponseHandler(
    private val connectedConsoleSocket: Socket,
    private val gSon: Gson
) {

    private val senderThread: Thread = Thread(RoomSenderRunnable())
    private val receiverThread: Thread = Thread(RoomReceiverRunnable())
    private val sendingMessageQueue: BlockingQueue<String> = ArrayBlockingQueue(10)
    private var lastKnowTimeForHeartBeat: Long = System.currentTimeMillis()
    private val outPrintWriter: PrintWriter = PrintWriter(
        BufferedWriter(OutputStreamWriter(connectedConsoleSocket.getOutputStream())),
        true
    )

    init {
        senderThread.start()
        receiverThread.start()

    }

    inner class RoomSenderRunnable : Runnable {
        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    sendingMessageQueue.poll()?.let {
                        sendMessage(it)
                    }
                    if (System.currentTimeMillis() - lastKnowTimeForHeartBeat > HEARTBEAT_SEND_TIME) {
                        sendingMessageQueue.put(gSon.toJson(Packet()))
                        lastKnowTimeForHeartBeat = System.currentTimeMillis()
                    }
                } catch (ie: InterruptedException) {
                    Log.e(REQUEST_RESPONSE_TAG, "Message sending loop interrupted, exiting")
                    tearDown()
                }
            }
        }
    }

    inner class RoomReceiverRunnable : Runnable {
        override fun run() {
            try {
                BufferedReader(InputStreamReader(connectedConsoleSocket.getInputStream())).use { bufferReader ->
                    var messageStr: String? = bufferReader.readLine()
                    while (messageStr != null && !Thread.currentThread().isInterrupted) {
                        Log.d(REQUEST_RESPONSE_TAG, "Read from the stream: $messageStr")
                        val message = gSon.fromJson(messageStr, Packet::class.java)
                        message.request?.let { request ->
                            when (request) {
                                SYNC_SETTINGS_REQUEST -> {
                                    Log.d(REQUEST_RESPONSE_TAG, "SYNC_SETTINGS_REQUEST")
                                    sendMessage(
                                        gSon.toJson(
                                            Packet(
                                                response = SEND_SETTINGS_RESPONSE, responseJson =
                                                gSon.toJson(Settings("Mercury", ""))
                                            )
                                        )
                                    )
                                }
                                else -> Log.d(REQUEST_RESPONSE_TAG, "Unknown request")
                            }
                        }
                        messageStr = bufferReader.readLine()
                    }
                }
            } catch (e: Exception) {
                Log.e(REQUEST_RESPONSE_TAG, "Server loop error: " + e.message)
                tearDown()
            }
        }
    }

    fun tearDown() {
        senderThread.interrupt()
        receiverThread.interrupt()
        try {
            outPrintWriter.close()
            connectedConsoleSocket.close()
        } catch (ioe: IOException) {
            Log.e(REQUEST_RESPONSE_TAG, "Error when closing room server socket.")
        }
    }


    fun sendMessage(msg: String) =
        try {
            outPrintWriter.println(msg)
            outPrintWriter.flush()
        } catch (e: Exception) {
            Log.d(REQUEST_RESPONSE_TAG, "Error while writing into the socket", e)
        }
}
