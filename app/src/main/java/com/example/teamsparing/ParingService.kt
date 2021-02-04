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

private const val ACTION = "com.microsoft.skype.teams.console.action"
private const val ACK = "com.microsoft.skype.teams.console.action.ack"

class ParingService: Service() {
    private lateinit var teamsParingReceiver: TeamsParingReceiver
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            stopSelf(msg.arg1)
        }
    }

    override fun onCreate() {
        super.onCreate()
        println("Sameer ParingService")

        HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }

        teamsParingReceiver = TeamsParingReceiver()
        val filter = IntentFilter()
        filter.addAction(ACTION)
        registerReceiver(teamsParingReceiver, filter)
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
        println("Sameer onDestroy")
        unregisterReceiver(teamsParingReceiver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}

class TeamsParingReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, intent: Intent?) {
        println("Sameer TeamsParingReceiver")
        intent?.let {currentIntent ->
            println("Sameer correlationId ${currentIntent.extras?.get("correlationId")}")
            println("Sameer actionDetails ${currentIntent.extras?.get("actionDetails")}")
            p0?.sendBroadcast(Intent(ACK).apply {
                extras?.putString("key","1612429043849")
            }) ?: println("Cannot Ack...")
        }
    }
}
