package com.example.connectionhandler.di

import com.example.connectionhandler.source.CommunicationChannel
import com.example.connectionhandler.source.CommunicationChannelImpl
import com.google.gson.Gson
import org.koin.dsl.module

val ConnectionHandlerModule = module {
    single { Gson() }
    single<CommunicationChannel> { CommunicationChannelImpl(get()) }
    //single { RoomServer(get()) } // This will start Server in both Room device and console. We have to find a way to start server only in case of room (If)
}
