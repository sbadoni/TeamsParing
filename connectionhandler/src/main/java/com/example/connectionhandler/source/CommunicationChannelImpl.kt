package com.example.connectionhandler.source

import android.os.Bundle
import android.os.Handler
import com.example.connectionhandler.data.PORT
import com.example.connectionhandler.data.Packet
import com.example.connectionhandler.data.SYNC_SETTINGS_REQUEST
import com.example.connectionhandler.data.SyncSettingsResponse
import com.google.gson.Gson
import java.net.InetAddress
import java.net.Socket


private const val TAG = "CommunicationChannel"

class CommunicationChannelImpl(private val gSon: Gson) : CommunicationChannel {

    private var consoleClient: ConsoleClient? = null
    private lateinit var paringServiceUpdateHandler: Handler
    private lateinit var roomServerSocket: Socket
    private var isUserInitiatedTearDown = false

    private val consoleCallbackHandler = object : ConsoleCallback {
        override fun updateSettingSyncResponse(result: Int, errorCode: String?) =
            updateMessages(createSettingSyncBundle(1, result, errorCode))

        override fun updateRoomServerConnectivity(isPaired: Int) {
            println("sameer isUserInitiatedTearDown $isUserInitiatedTearDown")
            if (isUserInitiatedTearDown.not()) {
                val messageBundle = Bundle()
                messageBundle.putInt("requestType", 2)
                messageBundle.putInt("isPaired", isPaired)
                updateMessages(messageBundle)
            }
        }
    }

    override fun init(paringServiceHandler: Handler) {
        paringServiceUpdateHandler = paringServiceHandler
        RoomServer(gSon)
    }

    override fun connectToRoomDevice(ipAddress: String) {
        try {
            setIsUserInitiatedTearDown(false)
            roomServerSocket = Socket(InetAddress.getByName(ipAddress), PORT)
            consoleClient = ConsoleClient(roomServerSocket, gSon, consoleCallbackHandler)
            sendMessage(gSon.toJson(Packet(SYNC_SETTINGS_REQUEST)))
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
        consoleClient?.putMessage(message)
    }

    override fun tearDown() {
        consoleClient?.tearDown()
        consoleClient = null
    }

    override fun isRoomConnected(): Boolean = consoleClient?.isRoomDeviceConnected ?: false

    override fun setIsUserInitiatedTearDown(flag: Boolean) {
        isUserInitiatedTearDown = flag
    }


    @Synchronized
    private fun updateMessages(messageBundle: Bundle) {
        val message = paringServiceUpdateHandler.obtainMessage()
        message.data = messageBundle
        paringServiceUpdateHandler.sendMessage(message)
    }

    private fun createSettingSyncBundle(requestType: Int, result: Int, errorCode: String?): Bundle =
        Bundle().apply {
            putInt("requestType", requestType)
            putInt("result", result)
            putString("errorCode", errorCode)
        }
}


