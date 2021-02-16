package com.example.connectionhandler.source

import android.os.Bundle
import android.os.Handler
import com.example.connectionhandler.data.PORT
import com.example.connectionhandler.data.Packet
import com.example.connectionhandler.data.SEND_SETTINGS_REQUEST
import com.example.connectionhandler.data.SyncSettingsResponse
import com.google.gson.Gson
import java.net.InetAddress
import java.net.Socket


private const val TAG = "CommunicationChannel"

class CommunicationChannelImpl(private val gSon: Gson) : CommunicationChannel {

    private var mCommunicationClient: CommunicationClient? = null
    private lateinit var mUpdateHandler: Handler
    private lateinit var serverSocket: Socket

    private val clientResponseHandler = object : ClientCallback {
        override fun updateSettingSyncResponse(result: Int, errorCode: String?) =
            updateMessages(createSettingSyncBundle(1, result, errorCode))

        override fun updateClientConnectivity(isPaired: Int) {
            val messageBundle = Bundle()
            messageBundle.putInt("requestType", 2)
            messageBundle.putInt("isPaired", isPaired)
            updateMessages(messageBundle)
        }

    }

    override fun init(updateHandler: Handler) {
        mUpdateHandler = updateHandler
        CommunicationServer(gSon)
    }

    override fun connectToServer(ipAddress: String) {
        try {
            serverSocket = Socket(InetAddress.getByName(ipAddress), PORT)
            mCommunicationClient = CommunicationClient(serverSocket, gSon, clientResponseHandler)
            sendMessage(gSon.toJson(Packet(SEND_SETTINGS_REQUEST)))
            mCommunicationClient?.let {
                Thread(it.ClientHeartBeatReceiver()).start()
            }
        } catch (e: Exception) {
            updateMessages(
                createSettingSyncBundle(
                    1,
                    SyncSettingsResponse.CANCELLED.ordinal,
                    e.message
                )
            )
        }
    }

    override fun sendMessage(message: String) {
        mCommunicationClient?.mMessageQueue?.put(message)
    }

    override fun tearDown() {
        mCommunicationClient?.tearDown()
    }

    override fun isRoomConnected(): Boolean = mCommunicationClient?.isServerConnected ?: false


    @Synchronized
    private fun updateMessages(messageBundle: Bundle) {
        val message = mUpdateHandler.obtainMessage()
        message.data = messageBundle
        mUpdateHandler.sendMessage(message)
    }

    private fun createSettingSyncBundle(requestType: Int, result: Int, errorCode: String?): Bundle =
        Bundle().apply {
            putInt("requestType", requestType)
            putInt("result", result)
            putString("errorCode", errorCode)
        }
}


