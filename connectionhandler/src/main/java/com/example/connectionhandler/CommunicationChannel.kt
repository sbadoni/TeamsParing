package com.example.connectionhandler

import android.os.Handler
import android.util.Log
import com.example.connectionhandler.data.Packet
import com.example.connectionhandler.data.Settings
import com.google.gson.Gson
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

const val PORT = 9999
private const val TAG = "CommunicationChannel"
private const val SEND_SETTINGS_REQUEST = "1"
private const val SEND_SETTINGS_RESPONSE = "2"

//Json parsing -Add
class CommunicationChannel(private val mUpdateHandler: Handler) {

    private var mCommunicationServer: CommunicationServer = CommunicationServer()
    private var mCommunicationClient: CommunicationClient? = null
    private val gson: Gson = Gson()

    fun connectToServer(address: InetAddress, port: Int) {
        val clientSocket = Socket(address, port)
        mCommunicationClient = CommunicationClient(clientSocket)
        if (clientSocket.isConnected) {
            println("client is connected ")
            mCommunicationClient?.let {
                it.mMessageQueue.put(gson.toJson(Packet(SEND_SETTINGS_REQUEST)))
            }
        } else{
            println("client not connected ")
        }
    }

    fun sendMessage(msg: String) {
        mCommunicationClient?.let {
            it.mMessageQueue.put("SEND ME SETTING...")
        }
    }


    inner class CommunicationServer {
        private var mServerSocket: ServerSocket? = null
        private var mThread: Thread? = Thread(CommunicationServerThread())

        init {
            println("$TAG init....")
            mThread?.start()
        }

        fun tearDown() {
            mThread?.let { it.interrupt() }
            try {
                mServerSocket?.let { it.close() }
            } catch (ioe: IOException) {
                Log.e(TAG, "Error when closing server socket.")
            }
        }

        inner class CommunicationServerThread: Runnable {
            override fun run() {
                try {
                    mServerSocket = ServerSocket(PORT)

                    while(!Thread.currentThread().isInterrupted){
                        Log.d(TAG, "ServerSocket Created, awaiting connection")
                        val client  = mServerSocket!!.accept()
                        Log.d(TAG, "Connected.")
                        client?.let { CommunicationClient(it) }
                    }
                } catch (e: Exception){
                    Log.e(TAG, "Error creating ServerSocket: ", e)
                }
            }
        }
    }


    inner class CommunicationClient(private val client: Socket) {
        private val CLIENT_TAG = "CommunicationClient"
        private val mSendThread: Thread? = Thread(SendingThread())
        private var mRecThread: Thread? = null
        var mMessageQueue: BlockingQueue<String> = ArrayBlockingQueue<String>(10)


        init {
            mSendThread?.start()
        }

        inner class SendingThread : Runnable {

            override fun run() {
                try {
                    mRecThread = Thread(ReceivingThread())
                    mRecThread!!.start()
                } catch (e: Exception){
                    Log.d(CLIENT_TAG, "Initializing socket failed, IOE.", e)
                }

                while (true) {
                    try {
                        val msg = mMessageQueue!!.take()
                        sendMessage(msg)
                    } catch (ie: InterruptedException) {
                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting")
                    }
                }
            }
        }

        inner class ReceivingThread: Runnable {

            override fun run() {
                var input: BufferedReader
                try {
                    input =  BufferedReader( InputStreamReader(
                        client.getInputStream()))
                    while (!Thread.currentThread().isInterrupted) {
                        var messageStr: String? = input.readLine()
                        if (messageStr != null) {
                            Log.d(CLIENT_TAG, "Read from the stream: $messageStr")
                            val message = gson.fromJson(messageStr, Packet::class.java)
                            message.request?.let { request ->
                                when (request) {
                                    SEND_SETTINGS_REQUEST -> {
                                        println("SEND_SETTINGS_REQUEST")
                                        sendMessage(
                                            gson.toJson(
                                                Packet(
                                                    response = SEND_SETTINGS_RESPONSE,
                                                    responseJson = gson.toJson(
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
                                            val settings = gson.fromJson(it, Settings::class.java)
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
                } catch (e: Exception){
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
            } catch (e: Exception){
                Log.d(CLIENT_TAG, "Error", e);
            }
        }
    }
}

