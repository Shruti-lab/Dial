package com.example.dialerapp.services

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog.Calls.PRESENTATION_ALLOWED
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dialerapp.utils.CallManager


class PhoneConnectionService: ConnectionService() {
    private val TAG = "PhoneConnectionService"
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?): Connection {
        val connection = PhoneConnection(this)
        connection.setAddress(request?.address ?: Uri.parse("tel:"), PRESENTATION_ALLOWED)
        connection.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED)
        connection.setConnectionCapabilities(Connection.CAPABILITY_SUPPORT_HOLD)
        connection.setCallerDisplayName("Shruti",PRESENTATION_ALLOWED)
        connection.setRinging()
        connection.setInitializing()
        connection.setDialing()

        // Register connection with CallManager
        CallManager.setCurrentConnection(connection)
        Log.d(TAG, "Outgoing call initiated")
        return connection

    }

    override fun onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?) {
        Log.e(TAG, "Failed to create outgoing connection - Address: ${request?.address}")
        // Notify CallManager about the failure
        CallManager.handleOutgoingCallFailed()
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?): Connection {
        Log.d(TAG, "Creating incoming connection")
        val connection = PhoneConnection(this)
        connection.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED)
        connection.setConnectionCapabilities(Connection.CAPABILITY_SUPPORT_HOLD)
        connection.setCallerDisplayName(request?.address?.schemeSpecificPart,PRESENTATION_ALLOWED)
        connection.setRinging()
        

        // Register connection with CallManager
        CallManager.setCurrentConnection(connection)
        Log.d(TAG, "Incoming call received")


        return connection
    }

    override fun onCreateIncomingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?) {
        Log.e(TAG, "Failed to create incoming connection")
        // Notify CallManager about the failure
        CallManager.handleIncomingCallFailed()
    }
}
