package com.example.teamsparing

import android.app.Application
import com.example.connectionhandler.di.ConnectionHandlerModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TeamsParingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        println("TeamsParingApplication")
        startKoin {
            androidContext(this@TeamsParingApplication)
            modules(ConnectionHandlerModule)
        }
    }
}
