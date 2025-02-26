package com.example.dialerapp.utils


import android.Manifest
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.ConnectionService.TELECOM_SERVICE
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dialerapp.screens.IncomingCallActivity
import com.example.dialerapp.screens.OutgoingCallActivity
import com.example.dialerapp.services.CallHandlingService
import com.example.dialerapp.services.PhoneConnection
import com.example.dialerapp.services.PhoneConnectionService

object CallManager {
    private var currentPhoneNumber: String? = null
    const val REQUEST_CALL_PHONE_PERMISSION: Int = 1
    private val TAG = "CALL MANAGER"
    private const val PHONE_ACCOUNT_LABEL = "Dialer App"
    private const val PHONE_ACCOUNT_ID = "DialerAppHandle"
    private var currentConnection: PhoneConnection? = null

//https://stackoverflow.com/questions/36576964/android-register-new-phoneaccount-for-telecom

    @RequiresApi(Build.VERSION_CODES.O)
    fun registerPhoneAccount(context: Context) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(context, PhoneConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, PHONE_ACCOUNT_ID)

        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, PHONE_ACCOUNT_LABEL)
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .build()

        val pa = telecomManager.getPhoneAccount(phoneAccountHandle)
        Log.d(TAG, pa?.toString() ?: "null")


        try {
            telecomManager.registerPhoneAccount(phoneAccount)
            Log.d(TAG, "Phone account registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register phone account: ${e.message}")
        }
    }

    fun initiateCall(context: Context, phoneNumber: String) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it from activity (handled in DialingScreen)
            Log.e(TAG,"CALL_PHONE permission was not given")
            ActivityCompat.requestPermissions(context as android.app.Activity,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE_PERMISSION);
            Log.d(TAG,"CALL_PHONE permission is GRANTED!!")
        }
        currentPhoneNumber = phoneNumber
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(context, PhoneConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, PHONE_ACCOUNT_ID)

        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
//            putBoolean(TelecomManager.EXTRA_CALL_BACK_NUMBER, true)
        }

        try {
            val uri = Uri.fromParts("tel", phoneNumber, null)
            telecomManager.placeCall(uri, extras)
            Log.d(TAG, "Initiating call to $phoneNumber")
        } catch (e: SecurityException) {
            Log.e(TAG, "Error placing call: ${e.message}")
            throw e
        }catch (e: Exception) {
            Log.e(TAG, "Unexpected error placing call: ${e.message}")
            throw e
        }

    }

    fun answerCall(incomingCallActivity: IncomingCallActivity) {
        currentConnection?.onAnswer()
        Log.d(TAG, "Call answered")

    }

    fun rejectCall(context: Context) {
        currentConnection?.onReject()
        Log.d(TAG, "Call rejected")
    }

    fun endCall(context: Context) {
        currentConnection?.onDisconnect()
        currentPhoneNumber = null
        currentConnection = null
        Log.d(TAG, "Call ended")
    }

    fun handleOutgoingCallFailed() {
        Log.e(TAG, "Outgoing call failed")
        currentPhoneNumber = null
        currentConnection = null
    }

    fun handleIncomingCallFailed() {
        Log.e(TAG, "Incoming call failed")
        currentPhoneNumber = null
        currentConnection = null
    }

    fun getCurrentPhoneNumber(): String? = currentPhoneNumber

    fun setCurrentConnection(connection: PhoneConnection) {
        if (currentConnection != null) {
            Log.w(TAG, "Overwriting existing connection")
            currentConnection?.onDisconnect()
        }
        currentConnection = connection
    }


}


