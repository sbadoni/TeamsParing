package com.example.connectionhandler.source

import android.util.Log
import com.example.connectionhandler.data.*
import com.google.gson.Gson
import java.io.*
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

private const val CONSOLE_TAG = "Console"
private const val HEARTBEAT_TIME_IN_SEC = 12 * 1000 //In Sec

class ConsoleClient(
    private val connectedRoomSocket: Socket,
    private val gSon: Gson,
    private val consoleCallback: ConsoleCallback
) {

    private val senderThread: Thread = Thread(SenderRunnable())
    private val receiverThread: Thread = Thread(ReceiverRunnable())
    private val heartBeatReceiver: Thread = Thread(ConsoleHeartBeatReceiver())
    private val sendingMessageQueue: BlockingQueue<String> = ArrayBlockingQueue(10)
    private val outPrintWriter: PrintWriter =
        PrintWriter(BufferedWriter(OutputStreamWriter(connectedRoomSocket.getOutputStream())), true)
    private var lastResponseTime: Long = System.currentTimeMillis()
    var isRoomDeviceConnected = true

    init {
        senderThread.start()
        receiverThread.start()
        heartBeatReceiver.start()
    }

    inner class SenderRunnable : Runnable {

        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val msg = sendingMessageQueue.take()
                    sendMessage(msg)
                } catch (ie: InterruptedException) {
                    tearDown()
                    Log.d(CONSOLE_TAG, "Message sending loop interrupted, exiting")
                }
            }
        }
    }

    inner class ReceiverRunnable : Runnable {
        override fun run() {
            try {
                BufferedReader(InputStreamReader(connectedRoomSocket.getInputStream())).use { bufferReader ->
                    var messageStr: String? = bufferReader.readLine()
                    while (messageStr != null && !Thread.currentThread().isInterrupted) {
                        Log.d(CONSOLE_TAG, "Read from the stream: $messageStr")
                        val message = gSon.fromJson(messageStr, Packet::class.java)
                        message.response?.let { response ->
                            when (response) {
                                SEND_SETTINGS_RESPONSE -> {
                                    message.responseJson?.let {
                                        val settings = gSon.fromJson(it, Settings::class.java)
                                        println("GOT: DEVICE-NAME ${settings.deviceName}")
                                        println("GOT: OTHER-PROPERTIES ${settings.otherProperties}")
                                        consoleCallback.updateSettingSyncResponse(
                                            SyncSettingsResponse.SUCCESS.ordinal,
                                            null
                                        )
                                    } ?: kotlin.run {
                                        consoleCallback.updateSettingSyncResponse(
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
                    }
                }
            } catch (e: Exception) {
                tearDown()
                Log.e(CONSOLE_TAG, "Server loop error: " + e.message)
            }
        }
    }

    private fun sendMessage(msg: String) =
        try {
            outPrintWriter.println(msg)
            outPrintWriter.flush()
        } catch (e: Exception) {
            Log.d(CONSOLE_TAG, "Error while writing into the socket", e)
        }

    fun tearDown() {
        senderThread.interrupt()
        receiverThread.interrupt()
        heartBeatReceiver.interrupt()
        try {
            outPrintWriter.close()
            connectedRoomSocket.close()
            isRoomDeviceConnected = false
        } catch (ioe: IOException) {
            Log.e(CONSOLE_TAG, "Error when closing server socket.")
        }
    }

    inner class ConsoleHeartBeatReceiver : Runnable {
        private var _loop = true
        override fun run() {
            try {
                while (_loop) {
                    println("System.currentTimeMillis() - lastResponseTime ${System.currentTimeMillis() - lastResponseTime}")
                    if (System.currentTimeMillis() - lastResponseTime > HEARTBEAT_TIME_IN_SEC) {
                        consoleCallback.updateRoomServerConnectivity(ParingStatus.UNPAIRED.ordinal)
                        tearDown()
                        _loop = false
                    }
                    Thread.sleep(4000)
                }

            } catch (e: Exception) {
                Log.e(CONSOLE_TAG, "Heartbeat interrupted")
            }
        }
    }

    fun putMessage(message: String) = sendingMessageQueue.put(message)
}

interface ConsoleCallback {

    fun updateSettingSyncResponse(result: Int, errorCode: String?)

    fun updateRoomServerConnectivity(isPaired: Int)
}

