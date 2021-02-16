package com.example.connectionhandler.source

import android.util.Log
import com.example.connectionhandler.data.*
import com.google.gson.Gson
import java.io.*
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

private const val CLIENT_TAG = "CommunicationClient"
private const val HEARTBEAT_TIME_IN_SEC = 12 * 1000 //In Sec

class CommunicationClient(
    private val serverSocket: Socket,
    private val gSon: Gson,
    private val clientCallback: ClientCallback
) {

    private val mSendThread: Thread = Thread(SendingThread())
    private var mRecThread: Thread = Thread(ReceivingThread())
    val mMessageQueue: BlockingQueue<String> = ArrayBlockingQueue(10)
    private var lastResponseTime: Long = System.currentTimeMillis()
    var isServerConnected = false

    init {
        mSendThread.start()
        mRecThread.start()
        isServerConnected = true
    }

    inner class SendingThread : Runnable {

        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val msg = mMessageQueue.take()
                    sendMessage(msg)
                } catch (ie: InterruptedException) {
                    tearDown()
                    Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting")
                }
            }
        }
    }

    inner class ReceivingThread : Runnable {
        override fun run() {
            try {
                BufferedReader(InputStreamReader(serverSocket.getInputStream())).use { bufferReader ->
                    var messageStr: String? = bufferReader.readLine()
                    while (messageStr != null && !Thread.currentThread().isInterrupted) {
                        Log.d(CLIENT_TAG, "Read from the stream: $messageStr")
                        val message = gSon.fromJson(messageStr, Packet::class.java)
                        message.response?.let { response ->
                            when (response) {
                                SEND_SETTINGS_RESPONSE -> {
                                    message.responseJson?.let {
                                        val settings = gSon.fromJson(it, Settings::class.java)
                                        println("GOT: DEVICE-NAME ${settings.deviceName}")
                                        println("GOT: OTHER-PROPERTIES ${settings.otherProperties}")
                                        clientCallback.updateSettingSyncResponse(
                                            SyncSettingsResponse.SUCCESS.ordinal,
                                            null
                                        )
                                    } ?: kotlin.run {
                                        clientCallback.updateSettingSyncResponse(
                                            SyncSettingsResponse.FAILED.ordinal,
                                            "Error while fetching the settings..."
                                        )
                                    }
                                }
                                else -> println("Unknown Response...")
                            }
                        }
                        messageStr = bufferReader.readLine()
                        lastResponseTime = System.currentTimeMillis()
                        println("sameer messageStr $messageStr")
                    }
                }
            } catch (e: Exception) {
                tearDown()
                Log.e(CLIENT_TAG, "Server loop error: " + e.message)
            }
        }
    }

    fun sendMessage(msg: String) {
        try {
            val socket = serverSocket
            if (socket.getOutputStream() == null) {
                Log.d(CLIENT_TAG, "Socket output stream is null");
            }

            val out = PrintWriter(
                BufferedWriter(
                    OutputStreamWriter(serverSocket.getOutputStream())
                ), true
            )
            out.println(msg)
            out.flush()
        } catch (e: Exception) {
            Log.d(CLIENT_TAG, "Error", e);
        }
    }

    fun tearDown() {
        mSendThread.interrupt()
        mRecThread.interrupt()
        try {
            serverSocket.close()
            isServerConnected = false
        } catch (ioe: IOException) {
            Log.e(CLIENT_TAG, "Error when closing server socket.")
        }
    }

    inner class ClientHeartBeatReceiver : Runnable {
        private var _loop = true
        override fun run() {
            while (_loop) {
                println("System.currentTimeMillis() - lastResponseTime ${System.currentTimeMillis() - lastResponseTime}")
                if (System.currentTimeMillis() - lastResponseTime > HEARTBEAT_TIME_IN_SEC) {
                    tearDown()
                    _loop = false
                }
                Thread.sleep(4000)
            }
            clientCallback.updateClientConnectivity(ParingStatus.UNPAIRED.ordinal)
        }
    }
}

interface ClientCallback {

    fun updateSettingSyncResponse(result: Int, errorCode: String?)

    fun updateClientConnectivity(isPaired: Int)
}

