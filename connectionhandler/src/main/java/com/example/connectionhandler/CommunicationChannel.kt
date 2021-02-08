package com.example.connectionhandler

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

const val PORT = 9999
private const val TAG = "CommunicationChannel"
class CommunicationChannel(private val mUpdateHandler: Handler) {

    private var mCommunicationServer: CommunicationServer = CommunicationServer()
    private var mCommunicationClient: CommunicationClient? = null

    fun tearDown() {
        //mCommunicationServer.tearDown()
        //mCommunicationClient?.let { it.tearDown() }
    }

    fun connectToServer(address: InetAddress, port: Int) {
        val clientSocket  = Socket(address, port)
        mCommunicationClient = CommunicationClient(clientSocket)
        if(clientSocket.isConnected){
            println("client is connected ")
            sendMessage("SEND ME SETTING...")
        }
        else{
            println("client not connected ")
        }
    }

    fun sendMessage(msg: String) {
        mCommunicationClient?.let {
            it.sendMessage(msg)
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
                       // if (mCommunicationClient == null) {
                        client?.let {
                                CommunicationClient(it)
                            }
                    //    }
                    }
                }
                catch (e: Exception){
                    Log.e(TAG, "Error creating ServerSocket: ", e)
                }
            }
        }
    }



    inner class CommunicationClient(private val client: Socket) {
        private val CLIENT_TAG = "CommunicationClient"
        private val mSendThread: Thread? = Thread(SendingThread())
        private var mRecThread: Thread? = null

        init {
            mSendThread?.start()
        }

        inner class SendingThread: Runnable {
            private val CAPACITY = 10
            var mMessageQueue: BlockingQueue<String>? = ArrayBlockingQueue<String>(CAPACITY)

            override fun run() {
                try {
                    mRecThread = Thread(ReceivingThread())
                    mRecThread!!.start()
                }
                catch (e: Exception){
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
                            //updateMessages(messageStr, false)
                        } else {
                            Log.d(CLIENT_TAG, "The null message...!")
                            break
                        }
                    }
                    input.close()
                }
                catch (e: Exception){
                    Log.e(CLIENT_TAG, "Server loop error: ", e);
                }
            }
        }

      /*  fun tearDown() {
            try {
                getSocket()!!.close()
            } catch (ioe: IOException) {
                Log.e(CLIENT_TAG, "Error when closing server socket.")
            }
        }*/

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
                //updateMessages(msg, true)
            }
            catch (e: Exception){
                Log.d(CLIENT_TAG, "Error", e);
            }
        }
    }
}

