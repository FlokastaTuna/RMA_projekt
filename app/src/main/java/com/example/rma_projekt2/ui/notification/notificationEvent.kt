package com.example.rma_projekt2.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.rma_projekt2.R


object NotificationEvent {

    private const val CHANNEL_ID = "catch_channel"
    private const val CHANNEL_NAME = "Catch Notifications"


    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /** Show the “Catch added” notification */
    fun showCatchAdded(context: Context) {
        ensureChannel(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Catch added")
            .setContentText("Your catch was saved successfully.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(1, notification)
    }
}