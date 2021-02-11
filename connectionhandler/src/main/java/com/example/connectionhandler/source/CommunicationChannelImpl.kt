package com.example.connectionhandler.source

import com.example.connectionhandler.data.PORT
import com.example.connectionhandler.data.Packet
import com.example.connectionhandler.data.SEND_SETTINGS_REQUEST
import com.google.gson.Gson
import java.net.InetAddress
import java.net.Socket


private const val TAG = "CommunicationChannel"

class CommunicationChannelImpl(
    private val gSon: Gson,
    private val CommunicationServer: CommunicationServer
) : CommunicationChannel {

    private var mCommunicationClient: CommunicationClient? = null

    override fun init() {
        println(TAG)
    }

    override fun connectToServer(ipAddress: String) {
        val clientSocket = Socket(InetAddress.getByName(ipAddress), PORT)
        mCommunicationClient = CommunicationClient(clientSocket, gSon)
        sendMessage(gSon.toJson(Packet(SEND_SETTINGS_REQUEST)))
    }

    override fun sendMessage(message: String) {
        mCommunicationClient?.mMessageQueue?.put(message)
    }

    override fun tearDown() {
        mCommunicationClient?.tearDown()
    }
}


