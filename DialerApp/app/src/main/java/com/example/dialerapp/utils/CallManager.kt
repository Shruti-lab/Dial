package com.example.dialerapp.utils


import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dialerapp.services.CallHandlingService

object CallManager {
    private var currentPhoneNumber: String? = null
    const val REQUEST_CALL_PHONE_PERMISSION: Int = 1


    fun initiateCall(context: Context, phoneNumber: String) {
        // Store the phone number for recording purposes
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it from activity (handled in DialingScreen)
            Log.e(TAG,"CALL_PHONE permission was not given")
            ActivityCompat.requestPermissions(context as android.app.Activity,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE_PERMISSION);
            Log.d(TAG,"CALL_PHONE permission is GRANTED!!")
        }

        currentPhoneNumber = phoneNumber

        // Start call state monitoring service
        val serviceIntent = Intent(context, CallHandlingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.e(TAG,"Starting service")
            context.startForegroundService(serviceIntent)  // Required for Android 8+
        } else {
            Log.e(TAG,"Starting service 8-")
            context.startService(serviceIntent)
        }


        // Create the call intent
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            Log.e(TAG,"calling!!!!!!!")
            data = Uri.parse("tel:$phoneNumber")
        }

        // Start the call
        context.startActivity(callIntent)
    }

    fun getCurrentPhoneNumber(): String? = currentPhoneNumber



    fun endCall(context: Context) {
        // Stop the service properly
        val serviceIntent = Intent(context, CallHandlingService::class.java)
        context.stopService(serviceIntent)

        // Clear the current call information
        currentPhoneNumber = null
    }

        // This should be called when we detect the call has ended
    fun handleCallEnded(context: Context) {
        endCall(context)
    }
}
