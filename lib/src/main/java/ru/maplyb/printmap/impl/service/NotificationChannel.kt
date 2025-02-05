package ru.maplyb.printmap.impl.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat

internal object NotificationChannel {
    val DOWNLOAD_CHANNEL_ID = "DOWNLOAD_CHANNEL_ID"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mapDownloadChannel(context)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun mapDownloadChannel(context: Context) {
        val name = "Download"
        val priority = NotificationManager.IMPORTANCE_HIGH
        val notificationChannel = NotificationChannel(DOWNLOAD_CHANNEL_ID, name, priority)
        NotificationManagerCompat.from(context).createNotificationChannel(notificationChannel)
    }
}