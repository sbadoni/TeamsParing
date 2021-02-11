package com.example.teamsparing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.connectionhandler.source.CommunicationChannel
import org.koin.android.ext.android.inject
import kotlin.concurrent.thread


private const val START_PARING_ACTION = "com.microsoft.skype.teams.console.action"
private const val ACK_ACTION = "com.microsoft.skype.teams.console.action.ack"
private const val TAG = "ParingService"

class ParingService : Service() {
    private var serviceLooper: Looper? = null
    private val mCommunicationChannel: CommunicationChannel by inject()
    private lateinit var mServiceHandler: Handler
    override fun onCreate() {
        super.onCreate()
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            mServiceHandler = object : Handler(serviceLooper!!) {
                override fun handleMessage(msg: Message) {
                }
            }
        }
        mCommunicationChannel.init()
        val filter = IntentFilter()
        filter.addAction(START_PARING_ACTION)
        registerReceiver(mTeamsParingReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("101", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                "101"
            }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("title")
            .setContentText("text")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
        startForeground(2001, notification)
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy")
        unregisterReceiver(mTeamsParingReceiver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private val mTeamsParingReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            intent?.let { intent ->
                when (intent.action) {
                    START_PARING_ACTION -> connectToRoom("192.168.29.212")
                    else -> println("Unknown Action")
                }
            }
        }
    }

    fun connectToRoom(ipAddress: String) {
        mCommunicationChannel.tearDown()
        thread { mCommunicationChannel.connectToServer(ipAddress) }
        sendBroadcast(Intent(ACK_ACTION))
    }
}
