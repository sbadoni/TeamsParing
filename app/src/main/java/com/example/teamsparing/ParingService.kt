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
import com.example.connectionhandler.data.ParingStatus
import com.example.connectionhandler.source.CommunicationChannel
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import kotlin.concurrent.thread

private const val CONSOLE_ACTION = "com.microsoft.skype.teams.console.action"
private const val CONSOLE_ACTION_RESULT = "com.microsoft.skype.teams.console.action.result"
private const val ACK_ACTION = "com.microsoft.skype.teams.console.action.ack"
private const val OEM_ACTION = "com.microsoft.skype.teams.console.oem.action"
private const val OEM_ACTION_NAME = "pairingStatusUpdated"
private const val CONSOLE_ACTION_NAME = "actionName"
private const val CONSOLE_ACTION_TYPE_PAIR = "pair"
private const val CONSOLE_ACTION_TYPE_UNPAIR = "unpair"
private const val CONSOLE_ACTION_DETAILS = "actionDetails"
private const val CONSOLE_ACTION_CORRELATION_ID = "correlationId"
private const val CONSOLE_ACTION_TYPE_CHECK_PARING_STATUS = "checkPairingStatus"
private const val TAG = "ParingService"

class ParingService : Service() {
    private var serviceLooper: Looper? = null
    private val mCommunicationChannel: CommunicationChannel by inject()
    private val gson: Gson by inject()
    private lateinit var mServiceHandler: Handler
    private var correlationId: String? = null
    override fun onCreate() {
        super.onCreate()
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
            serviceLooper = looper
            mServiceHandler = object : Handler(serviceLooper!!) {
                override fun handleMessage(msg: Message) {
                    when (msg.data.getInt("requestType")) {
                        1 -> {
                            val result = msg.data.getInt("result")
                            val errorCode = msg.data.getString("errorCode")
                            println("result = $result errorCode = $errorCode")
                            sendBroadcast(Intent(CONSOLE_ACTION_RESULT).apply {
                                putExtra("correlationId", correlationId)
                                putExtra("result", result)
                                putExtra("errorCode", errorCode)
                            })
                        }
                        2 -> {
                            val isPaired = msg.data.getInt("isPaired")
                            println("isPaired = $isPaired")
                            sendBroadcast(Intent(OEM_ACTION).apply {
                                putExtra("correlationId", correlationId)
                                putExtra("actionName", OEM_ACTION_NAME)
                                putExtra("pairingStatus", isPaired)
                            })
                        }
                        else -> println("Unknown ")
                    }


                }
            }
            mCommunicationChannel.init(mServiceHandler)
        }
        val filter = IntentFilter()
        filter.addAction(CONSOLE_ACTION)
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
            println("sameer ${intent?.action}")
            intent?.let { intent ->
                when (intent.action) {
                    CONSOLE_ACTION -> {
                        val actionType = intent.getStringExtra(CONSOLE_ACTION_NAME)
                        if (actionType == CONSOLE_ACTION_TYPE_PAIR) {
                            val actionDetails = intent.getStringExtra(CONSOLE_ACTION_DETAILS)
                            actionDetails?.let { details ->
                                correlationId = intent.getStringExtra(CONSOLE_ACTION_CORRELATION_ID)
                                connectToRoom(gson.fromJson(details, ParingDetail::class.java))
                            }
                        }
                        if (actionType == CONSOLE_ACTION_TYPE_CHECK_PARING_STATUS) {
                            checkParingStatus()
                        }

                        if (actionType == CONSOLE_ACTION_TYPE_UNPAIR) {
                            unPairConnection()
                        }

                    }
                    else -> println("Unknown Action")
                }
            }
        }
    }

    private fun connectToRoom(paringDetails: ParingDetail) {
        mCommunicationChannel.tearDown()
        thread { mCommunicationChannel.connectToServer(paringDetails.roomDeviceIPAddress) }
        sendBroadcast(Intent(ACK_ACTION))
    }

    private fun checkParingStatus() =
        sendBroadcast(Intent(CONSOLE_ACTION_RESULT).apply {
            putExtra("correlationId", correlationId)
            val checkRoom = mCommunicationChannel.isRoomConnected()
            println("sameer checkRoom $checkRoom")
            putExtra(
                "result",
                if (mCommunicationChannel.isRoomConnected()) ParingStatus.PAIRED.ordinal else ParingStatus.UNPAIRED.ordinal
            )
        })

    private fun unPairConnection() {
        sendBroadcast(Intent(ACK_ACTION).apply {
            putExtra("correlationId", correlationId)
            putExtra("actionName", OEM_ACTION_NAME)
        })
        mCommunicationChannel.tearDown()
    }
}
