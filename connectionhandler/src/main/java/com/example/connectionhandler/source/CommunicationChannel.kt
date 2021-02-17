package com.example.connectionhandler.source

import android.os.Handler

interface CommunicationChannel {

    fun init(paringServiceHandler: Handler)

    fun connectToRoomDevice(ipAddress: String)

    fun sendMessage(message: String)

    fun tearDown()

    fun isRoomConnected(): Boolean

    fun setIsUserInitiatedTearDown(flag: Boolean)
}
