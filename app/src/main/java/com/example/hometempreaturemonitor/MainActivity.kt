package com.example.hometempreaturemonitor

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import kotlin.concurrent.thread
import android.os.PowerManager

import android.os.PowerManager.WakeLock
import android.app.KeyguardManager.KeyguardLock

import android.app.KeyguardManager
import android.util.Log
import java.lang.Exception


private const val CHANNEL_DEFAULT_IMPORTANCE = "Channel_Id"
private const val ONGOING_NOTIFICATION_ID = 1


class ServerService : Service() {
    private var context: Context? = null
    private var serviceLooper: Looper? = null
    private var socketServer: SocketServer? = null
    private var temperatureDatabase: TemperatureDataStorage? = null

    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        context = this
        socketServer = SocketServer()
        socketServer?.init()
        TemperatureDataStorage.instance.init(applicationContext)
    }

    private fun keepDeviceAlive() {
        while (true) {
            try {
                wakeUp()
            } catch (ex: Exception) {
                Log.e("main", "Could not wake the device")
            }
            Thread.sleep(1000L)
            try {
                releaseScreen()
            } catch (ex: Exception) {
                Log.e("main", "Could not release the screen")
            }
            Thread.sleep(10 * 60 * 1000L /*10 minutes*/)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        thread { socketServer?.run() }
        thread { TelegramBotWrapper.instance.main() }
        thread { keepDeviceAlive() }
        thread { TemperatureWarnMonitor().main() }
        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    private fun wakeUp() {
        Log.i("main", "Trying to wake the device")
        val pm: PowerManager = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "hometempreaturemonitor:mainwake"
        )
        wakeLock.acquire(1000L);
    }

    private fun releaseScreen() {
        Log.i("main", "Releasing the device")
        val keyguardManager =
            applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val keyguardLock = keyguardManager.newKeyguardLock("hometempreaturemonitor:mainwake")
        keyguardLock.disableKeyguard()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForeground() {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notification: Notification = Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.ticker_text))
            .build()

// Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Intent(this, ServerService::class.java).also { intent ->
            startService(intent)
        }
    }
}