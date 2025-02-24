package com.example.dialerapp.services

import android.accessibilityservice.AccessibilityService
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.CALL_STATE_IDLE
import android.telephony.TelephonyManager.CALL_STATE_OFFHOOK
import android.telephony.TelephonyCallback.CallStateListener
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat


class CallHandlingService : Service() {
    private var telephonyManager: TelephonyManager? = null
    private lateinit var telephonyCallback: TelephonyCallback
    private lateinit var phoneStateListener: PhoneStateListener


    private val CHANNEL_ID = "CallHandlingChannel"
    private val NOTIFICATION_ID = 1


    override fun onCreate() {
        super.onCreate()
        startForeground()

        startListeningForCallState()


    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY // Service will stop when no longer needed
    }

    private fun startForeground() {

        try {
            val notification = NotificationCompat.Builder(this, "ForegroundServiceChannelId")
                // Create the notification to display while the service is running
                .setContentTitle("Call Handling Service")
                .setContentText("Monitoring call state...")
                .build()

            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ 100,
                /* notification = */ notification,
                /* foregroundServiceType = */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                return
            }

        }
    }



    private fun startListeningForCallState() {
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        // For devices with API level 31 and above, use TelephonyCallback
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(), CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    Log.d("CallHandlingService", "Call state changed: $state")
                    handleCallState(state)

                }
            }
            telephonyManager?.registerTelephonyCallback(mainExecutor, telephonyCallback)
        } else {
            // For devices below API level 29 (Android 10), use PhoneStateListener
            phoneStateListener = object : PhoneStateListener() {
                @RequiresApi(Build.VERSION_CODES.O)
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state)
                }
            }
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }


private fun handleCallState(state: Int) {
    Log.d("CallHandlingService", "Call state changed: $state")
    when (state) {
        CALL_STATE_OFFHOOK -> {
//            if (Build.VERSION.SDK_INT in 24..28) {
//                startRecordingWithAudioRecord()
//            }

//            startForegroundService(recordIntent)
            if (Build.VERSION.SDK_INT >= 29) {
                if (!isAccessibilityServiceEnabled(this, CallAccessibilityService::class.java)) {
                    Log.e("CallHandlingService", "Accessibility Service is NOT enabled. Prompting user...")
                    showAccessibilityPrompt()
                } else {
                    Log.d("CallHandlingService", "Accessibility Service is enabled. It will handle recording.")
                }
            } else {
                Log.d("CallHandlingService", "Call started - Starting Recording Service")
                val recordIntent = Intent(this, CallRecordingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.e(TAG,"Starting service")
                    startForegroundService(recordIntent)  // Required for Android 8+
                } else {
                    Log.e(TAG,"Starting service 8-")
                    startService(recordIntent)
                }

            }

        }
        CALL_STATE_IDLE -> {


            // Force stop the accessibility service to ensure it releases resources
            if (Build.VERSION.SDK_INT >= 29) {
                Log.d("CallHandlingService", "Call ended - Stopping Accessibility Recording Service")
                stopService(Intent(this, CallAccessibilityService::class.java))
            }else{
                Log.d("CallHandlingService", "Call ended - Stopping Recording Service")
                val stopIntent = Intent(this, CallRecordingService::class.java)
                stopService(stopIntent)
            }
        }
    }
}

    // ✅ Check if Accessibility Service is enabled
    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val colonSplitter = enabledServices?.split(":") ?: return false
        return colonSplitter.any { it.contains(service.name) }
    }

    // ✅ Show prompt to enable Accessibility Service
    private fun showAccessibilityPrompt() {
        Toast.makeText(this, "Enable Accessibility Service for call recording", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CallHandlingService", "onDestroy called")
        // For devices with API level 29 and above (Android 10 and above), unregister the TelephonyCallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager?.unregisterTelephonyCallback(telephonyCallback)
        }
        // For devices below API level 29 (Android 10), unregister the PhoneStateListener
        else {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }


        if (Build.VERSION.SDK_INT >= 29) {
            stopService(Intent(this, CallAccessibilityService::class.java))
        }else{
            stopService(Intent(this, CallRecordingService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

}

