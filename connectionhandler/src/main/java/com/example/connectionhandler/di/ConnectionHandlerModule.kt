package com.example.connectionhandler.di

import com.example.connectionhandler.source.CommunicationChannel
import com.example.connectionhandler.source.CommunicationChannelImpl
import com.example.connectionhandler.source.CommunicationServer
import com.google.gson.Gson
import org.koin.dsl.module

val ConnectionHandlerModule = module {
    single { Gson() }
    single<CommunicationChannel> { CommunicationChannelImpl(get(), get()) }
    single { CommunicationServer(get()) }
}
