package com.example.pushupscounter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat


class ScreenRecordService : Service() {


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val notificationChannel = NotificationChannel(
                "ScreenRecordChannel",
                "Screen recording channel",
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager = getSystemService(
                NotificationManager::class.java
            )

            notificationManager.createNotificationChannel(notificationChannel)

            val notificationBuilder = NotificationCompat.Builder(
                this,
                "ScreenRecordChannel"
            )

            val notification = notificationBuilder
                .setContentTitle("Screen recording")
                .setContentText("Recording pushups count")
                .setSmallIcon(R.drawable.pushups)
                .build()

            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }



        //handleCommand(intent)
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        //return START_STICKY
        return super.onStartCommand(intent, flags, startId)
    }
}