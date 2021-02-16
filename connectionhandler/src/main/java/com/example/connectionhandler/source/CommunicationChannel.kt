package com.example.connectionhandler.source

import android.os.Handler

interface CommunicationChannel {

    fun init(updateHandler: Handler)

    fun connectToServer(ipAddress: String)

    fun sendMessage(message: String)

    fun tearDown()

    fun isRoomConnected(): Boolean
}
