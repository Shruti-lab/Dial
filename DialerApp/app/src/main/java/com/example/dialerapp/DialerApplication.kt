package com.example.dialerapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class DialerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Create the notification channel (required for Android 8.0 and above)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val callHandlingChannel = NotificationChannel(
                "ForegroundServiceChannelId",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

                //Separate channel for AccessibilityService
            val accessibilityChannel = NotificationChannel(
                "AccessibilityServiceChannelId",
                "Call Accessibility Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // service provided by Android Operating system to show notification outside of our app
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(callHandlingChannel)
            notificationManager.createNotificationChannel(accessibilityChannel)

        }
    }
}