package com.example.dialerapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dialerapp.utils.CallManager


class DialerApplication : Application() {
    private val TAG = "DIALERAPPLICATION"
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "DialerApplication onCreate started")

        // Register phone account for our dialer app
        CallManager.registerPhoneAccount(this)
        Log.d(TAG, "registering phone acc done")


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

            val incomingCallChannel = NotificationChannel("IncomingCallChannelId", "Incoming Calls", NotificationManager.IMPORTANCE_HIGH)
            incomingCallChannel.enableLights(true)
            incomingCallChannel.enableVibration(true)
            incomingCallChannel.setShowBadge(true)

            // Create a call style notification for an incoming call.
//            val builder = Notification.Builder(context, CHANNEL_ID)
//                .setContentIntent(contentIntent)
//                .setSmallIcon(smallIcon)
//                .setStyle(
//                    Notification.CallStyle.forIncomingCall(caller, declineIntent, answerIntent))
//                .addPerson(incomingCaller)

            // using default system ringtone for our incoming call notification channel
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            incomingCallChannel.setSound(
                ringtoneUri,
                AudioAttributes.Builder() // Setting the AudioAttributes is important as it identifies the purpose of your
                    // notification sound.
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )


            // service provided by Android Operating system to show notification outside of our app
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(callHandlingChannel)
//            notificationManager.createNotificationChannel(accessibilityChannel)
            notificationManager.createNotificationChannels(listOf(incomingCallChannel, callHandlingChannel, accessibilityChannel))

        }
    }
}