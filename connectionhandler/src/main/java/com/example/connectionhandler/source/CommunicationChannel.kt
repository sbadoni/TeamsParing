package com.example.connectionhandler.source

interface CommunicationChannel {

    fun init()

    fun connectToServer(ipAddress: String)

    fun sendMessage(message: String)

    fun tearDown()
}
